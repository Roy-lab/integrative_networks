"""
Get top-ranked regulators for each lambda, for a module.
Use regression weights.
Compare to random runs - accept regulators with significantly higher weights in
real than random, by z-test. Threshold is p<0.05 corrected by the number of regulators
with nonzero weights in the real data.

grep ^0.10 mod1424_regulators.tab | sort -k 4 | awk '($5 > 0 && ($3 > 0.1 || $3 < -0.1)) {print $0}'


Usage:
get_regulators_from_weights.py results_directory moduleID cutoff> moduleID_regulators.tab
just use the module number
cutoff: recommended is 0.10 for mouse, 0.20 for human

Number of random runs, significance threshold (Bonferroni corrected), is hardcoded here.

"""
import sys, os, csv
import numpy as np
import scipy.stats as st
import math 

def main(argv):
	if len(argv) != 4:
		print "Usage: get_regulators_from_weights.py results_dir moduleID cutoff > moduleID_regulators.tab"
		print "Results dir is location of the regweights files"
		print "Module ID should be a number, like 1549"
		print "Cutoff is for the absolute regression weight (eg 0.20)"
		return 2
	mod=argv[2]
	rdir=argv[1]	
	cutoff=float(argv[3])

	realfile="%s/module%s_regweights.tab" % (rdir, mod)
	if not os.path.exists(realfile):
		print >> sys.stderr, "Module results file %s doesn't exist." % realfile
		return 1
	real_data=read_weight_file(realfile)
	
	numrand= 40
	
	alpha=0.05	# p-value cutoff - will correct based on number of tests
	
	# compare random weights to the cutoff - call false discovery if 
	# cannot say that the random weights are below 0.10 at corrected p-value
	
	rankings={}	# { lambda : [ (reg, score) ]
	# for each lambda
	for (lam, data) in real_data.items():
		# get scores for each regulator: average target score across module		
		scores=score_regs_for_one_lambda(data)
				
		# sort by absolute avg magnitude 
		regs=sorted(scores.items(), key=lambda x: -1*abs(x[1]))
		rankings[lam]=regs
		
				
	rand_scores=dict( [ (lam, {}) for lam in rankings] ) # { lambda : {regulator : [ scores ] }}
	# read in random data
	for i in range(1, numrand+1):
		fn="%s/module%s_rand%d_regweights.tab" % (rdir, mod, i)
		# break out if not available
		if not os.path.exists(fn):
			print >> sys.stderr, "Random file %d doesn't exit for module %s; aborting." % (i, mod)
			return 1
		
		rdata=read_weight_file(fn)
		for (lam, rd) in rdata.items():
			rscores=score_regs_for_one_lambda(rd)
			for (reg,score) in rscores.items():
				if reg not in rand_scores[lam]:
					rand_scores[lam][reg]=[]
				rand_scores[lam][reg].append(score)
				
	
	print "#lambda\tprotein\tavg_reg_weight\tp-val(random < %.2f)\tweight>%.2f and reject h0" % (cutoff, cutoff)
	for (lam, rscores) in rankings.items():
		# which have higher scores than expected by random?
		
		# corrected p-value threshold
		pcorr=alpha/len(rscores)
		
		compared={}	# {reg : (score, pval, hit-call) } from one-sided test
		
		# report false discovery rate at cutoff
		allpos=0
		fpos=0
		# for each real score above the cutoff	
		for (reg, score) in rscores:
		
			rand_mean = np.mean(rand_scores[lam].get(reg,[0]))
			rand_sd = np.std(rand_scores[lam].get(reg,[0]))									
			se=(rand_sd/math.sqrt(numrand))
			
			# compare random scores to the cutoff
			if rand_sd>0:
				z=(abs(rand_mean)-abs(cutoff))/se
				pval=st.norm.cdf(z)
			else:
				pval=0.0
			
			# call hit if random less than cutoff with p < corrected alpha
			# AND absolute real magnitude above cutoff
			if pval <= pcorr and abs(score) > cutoff:
				hitcall=1
			else:
				hitcall=0			
			compared[reg]=(score, rand_mean, pval, hitcall)
			
			if abs(score) >= cutoff:
				allpos+=1
				if pval > pcorr:
					fpos+=1
			
			#print "%s\t%.2f\t%.2f\t%d" % (reg, score, pval, hitcall)
			
		fdr="NA"
		if allpos > 0:
			fdr="%.3f" % (float(fpos)/allpos)
		
		
		print "# lambda %.2f FDR %s %d/%d" % (lam, fdr, fpos, allpos)
		for (reg, results) in compared.items():
			(score, rand_mean, pval, hitcall) = results
			print "%s\t%f\t%s\t%.2f\t%.2f\t%.3f\t%d" % (mod, lam, reg, score, rand_mean, pval, hitcall)
			
	return
				
	
	
	# how does ranking change as lambda increases?
	sortlam=sorted(rankings.keys())
	maxct=max( [len(v) for v in rankings.values() ])
	for i in range(maxct):
		line=[]
		for l in sortlam:
			regs=rankings[l]
			if len(regs) > i:
				line.append("%s:%.2f" % regs[i])
			else:
				line.append("")
		print "\t".join(line)				

def score_regs_for_one_lambda(data):
	"""
	Gets a single score for each regulator:
	Average absolute regression weight across genes in the module (also across the folds)
	
	Input:  { protein : { target : { fold : weight} }}
	Output: { protein : score }
	"""
	totals={} # {protein : sum_abs(avg_weight)}
	avgs={}	# {protein : average total over number of targets}
	for (p, tmap) in data.items():
		totals[p]=0
		for (t, vals) in tmap.items():
			# average weight for this protein-target pair
			# not absolute - need this to be consistent across folds
			# to consider it meaningful.
			avg=sum( [ w for (f,w) in vals.items() ] )/float(len(vals))
			
			# we also care about sign at the module level - better regulators
			# will be consistent across module genes?
			totals[p] += avg
			
		avgs[p]=totals[p]/float(len(tmap))
	return avgs
		

	

def read_weight_file(fn):
	"""
	Reads regression weight file for one module.
	Input format: lambda\tfold\tprotein\ttarget\tweight
	Output format:
	{ lambda : { protein : { target : { fold : weight} }}}		
	"""
	results={}
	fields=["lambda","fold","protein","target","weight"]
	fmt=[float, int, str, str, float]
	with open(fn) as f:
		reader=csv.reader(f, delimiter="\t")
		for row in reader:
			# format each field according to our specifications
			fmtrow= [ fmt[i](row[i]) for i in range(len(row))] 
			lam=fmtrow[0]
			fold=fmtrow[1]
			prot=fmtrow[2]
			tar=fmtrow[3]
			wt=fmtrow[4]
			
			if lam not in results:
				results[lam]={}
			if prot not in results[lam]:
				results[lam][prot]={}
			if tar not in results[lam][prot]:
				results[lam][prot][tar]={}
				
			# One weight per protein->target per fold
			results[lam][prot][tar][fold]=wt		
		
	return results	

if __name__=="__main__":
	sys.exit(main(sys.argv))
