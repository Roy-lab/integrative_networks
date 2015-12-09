# runs human lasso jobs - wait 6 hours between queueing each suffix
#M=$1

#if [[ $# != 1 ]]; then
#	echo "Please specify a file suffix"
#	exit
#fi

if [[ ! -d ~/influenza_rmse/human_lasso_rmse/ ]]; then
	mkdir -p ~/influenza_rmse/human_lasso_rmse/
fi

suffixes="aa bb cc dd"

for M in $suffixes; do
	matlab -nodisplay -nodesktop -r "mfile='human_inputs/humrse${M}'; regfile='human_inputs/human_prot_impute_0.50miss_noheader.txt'; indir='human_inputs'; outdir='~/influenza_rmse/human_lasso_rmse/human_lasso_rmse/'; run bash_run_lasso_v2.m, quit()" > ~/influenza_rmse/human_lasso_rmse/human_lasso_rmse_${M}.log &
	
	sleep 6h
done

