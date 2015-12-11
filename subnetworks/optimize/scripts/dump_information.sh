# dumps node, edge, path relevance variables
# from gdx files in directory $1 with prefix $2
# into a directory specified by $3
# then summarizes into three files with prefix $2

if [ $# -ne 3 ]; then
	echo "Usage: dump_information.sh gdx_dir prefix_of_gdx_files output_dir"
	exit
fi

gdxdir=$1
pref=$2
outdir=$3

if [[ ! -e $outdir ]]; then
	echo "Making output directory"
	mkdir $outdir
fi

echo "Deleting existing dump files"
rm -f ${outdir}/*_dump

for fn in ${gdxdir}/${pref}*gdx
do
	f=${fn##*/}  # remove path
	f=${f%.*} # remove extension
	# indexed by single item
	for i in sigma x y d 
	do
		gdxdump ${gdxdir}/${f}.gdx symb=${i} Format=csv | grep -v ",0" | grep -v "Val" | awk -v i="${i}" -F"," '{print 	i"\t"$1"\t"$2}' | sed 's/"//g' >> ${outdir}/${f}_dump
	done
	for i in sat
	do
		gdxdump ${gdxdir}/${f}.gdx symb=${i} Format=csv | grep -v ",0" | grep -v "Val" | awk -v i="${i}" -F"," '{print 	i"\t"$1"\t"$2"\t"$3}' | sed 's/"//g' >> ${outdir}/${f}_dump
	done
		
done


# Make tab-delim files with conf scores
python scripts/gather_path_solution_info.py "${outdir}/*_dump" ${outdir}/${pref}


