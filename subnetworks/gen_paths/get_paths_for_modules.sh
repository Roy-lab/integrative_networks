# Generate candidate paths for all modules in input file.
base=configs/influenza_module_replace.config
modules=$1 #human_modules.tab or test_mod.tab  #list of modules, one per line

while read m
do
	config=configs/influenza_module_${m}.config	
	sed s/{MODULE}/${m}/g ${base} > ${config}
	java -jar influenza_subnet.jar ${config} > gather_module_${m}.log
	echo "Finished module $m" 
done < $modules
