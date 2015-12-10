# README #

Code accompanying the manuscript for integrative analysis of host transcriptomic/proteomic response to pathogens.

Contains two code modules:

* A. MTG-LASSO to infer protein regulators of gene expression modules
* B. Physical subnetwork inference to connect predicted regulators for modules

# A. MTG-LASSO to infer protein regulators of gene expression modules #
### Requirements  
Written with MATLAB 2014b, [SLEP v4.1](http://www.yelab.net/software/SLEP/), Python 2.7, bash.

### Step 1: Run MTG-LASSO/LASSO to learn regression weights for regulators to targets.

The bash script, **infer_protein_regs.sh**, allows the user to run protein regulator inference using either MTG-LASSO or LASSO once the following are specified:

* Location of protein data file (text, tab-delimited matrix)
* Location of gene expression module files (one file per module)
* Output directory

### Step 2: Extract high-confidence protein regulators.
We extract high-confidence protein regulators based on frequency across folds as well as setting a threshold for regression weight and testing that the protein is not randomly assigned that weight under permutations of the protein data.
Main script for that step: **get_highconf_regs.sh**

# B. Physical subnetwork inference to connect predicted regulators for modules #
### Requirements
Java, Python 2.7, GAMS, CPLEX, bash.

### Step 1. Generate candidate paths to connect regulators for each module (gen_paths)
Finds all directed paths up to a given length that connect 'upstream' MERLIN and MTG-LASSO regulators to 'downstream' MERLIN-identified transcription factors.

Main script: **gen_paths/get_paths_for_modules.sh**

Source code for influenza_subnet.jar is in the src/ directory. Main class: apps/InfluenzaMain

Produces GAMS-ready files in directory gams_input/.

### Step 2. Optimize ILP to identify sparse subnetwork for each module (optimize)

Main script: