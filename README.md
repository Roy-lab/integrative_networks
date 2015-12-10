# README #

Code accompanying the manuscript for integrative analysis of host transcriptomic/proteomic response to pathogens.

## MTG-LASSO to infer protein regulators of gene expression modules ##
### Requirements  
Written with MATLAB 2014b and [SLEP v4.1](http://www.yelab.net/software/SLEP/).

### Step 1: Run MTG-LASSO/LASSO to learn regression weights for regulators to targets.

The bash script, **infer_protein_regs.sh**, allows the user to run protein regulator inference using either MTG-LASSO or LASSO once the following are specified:

* Location of protein data file (text, tab-delimited matrix)
* Location of gene expression module files (one file per module)
* Output directory

### Step 2: Extract high-confidence protein regulators.
We extract high-confidence protein regulators based on frequency across folds as well as setting a threshold for regression weight and testing that the protein is not randomly assigned that weight under permutations of the protein data.
Main script for that step: **get_highconf_regs.sh** 