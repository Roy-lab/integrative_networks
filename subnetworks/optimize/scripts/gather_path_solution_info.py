"""
Collects the variable info in a directory of dumped gdx files and 
produces summary files:
 path confidence, node confidence, and edge confidence
 edge direction(?)
 pair satisfaction....
"""
import sys
import glob

# argv 1: pattern for matching dump files; eg: path_sol*dump
ls = glob.glob(sys.argv[1])

outpref = sys.argv[2]
print "Writing confs to files with prefix %s" % outpref

# number of times we see a path
paths={}
nodes={}
edges={}
dirs={}
pairs={}
gomap = {"sigma":paths, "x":edges, "y":nodes, "d":dirs, "sat":pairs}

tot=0
for fn in ls:
	#print "#", fn
	bad=False
	
	seen=set()
	with open(fn) as f:
		for line in f:
			# didn't work
			if "Symbol not found" in line:
				print >> sys.stderr, "Bad file: %s" % fn
				bad=True
				break
			
			# var\t"name"\tsetting
			sp=line.strip().split("\t")
			symbol = ".".join(sp[1:-1]).replace("'","")
			#print symbol

			if symbol in seen:
				continue
			try:
				setting = int(sp[-1])		
			except (IndexError, ValueError) as e:
				print >> sys.stderr, "Bad format", fn, sp
				bad=True
				break

			if sp[0] not in gomap:
				print >> sys.stderr, "Implementation for variable %s not available" % sp[0]
			themap = gomap[sp[0]]
			themap[symbol] = themap.get(symbol,0)+1
		if not bad:
			tot+=1

print "# %d total solutions" % tot

total=float(tot)

for var in gomap.keys():
	with open("%s_%s.tab" % (outpref, var), "w") as f:
		f.write("#item\tconf(%s)\n" % var)
		for (s, count) in gomap[var].items():
			conf=0
			if total > 0:
				conf=count/total
			f.write("%s\t%f\n" % (s, conf))
			if conf > 1:
				print >> sys.stderr, "Too many entries per symbol in your dump files. (%s, %d, %d)" % (s, count, total)
				sys.exit()




