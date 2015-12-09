# README #

Code accompanying the manuscript for integrative analysis of host transcriptomic/proteomic response to pathogens.

## MTG-LASSO to infer protein regulators of gene expression modules ##
### Requirements  
Written with MATLAB 2014b and [SLEP v4.1](http://www.yelab.net/software/SLEP/).

### Main script 
The bash script, infer_protein_regs.sh, allows the user to run protein regulator inference using either MTG-LASSO or LASSO once the following are specified:

* Location of protein data file (text, tab-delimited matrix)
* Location of gene expression module files (one file per module)
* Output directory