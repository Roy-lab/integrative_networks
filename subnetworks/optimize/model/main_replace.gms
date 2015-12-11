* Template for running CPLEX to solve the subnetwork ILP for a module.
* Infer directed paths between regulators and host factors, including Watanabe hits:
* 1) Maximize connections between MERLIN regulators and protein regulators
* 2) Minimize intermediate nodes, with no penalty for including Watanabe hits.
* 3) Maximize paths between nodes in each solution (just to fill in edges)
* Final version: allow one intermediate between pairs of regulators.

* Fix identity of nodes and then maximize paths.
* Parameters are indicated with {.}:
*	NSOL	number of solutions 
*	SETFN	set file of network info
*	OUTPREF	final GDX filename prefix

$phantom null

Variable budget	"budget for unannotated nodes";

Scalar prob "sample this fraction of sols" /1.0/;

set 	soln 	possible solutions in the solution pool 
	/file1*file{NSOL}/;

$Include {SETFN}

$phantom null

* We may want to use this, but don't currently
Set special(edge)	/ null /;

$Include model.gms

* include special edges, if they exist
x.fx(special(edge))=1;

Set dp(node,node)	"directed edges, nodes only";
dp(node,node1)=no;
loop( enode(edge, node, node1)$(not ppi(edge)), dp(node,node1)=yes);

* if both dir and ppi available, deactivate ppi 
* (this shouldn't actually need to happen)
Set deactivate(ppi);
deactivate(ppi)=no;
loop( enode(ppi, node, node1)$(dp(node,node1) or dp(node1,node)),
	deactivate(ppi)=yes;);
x.fx(deactivate)=0;

************Build and solve!*****************

* must include protein regs and MERLIN regs
Set reg(node) "regulators";
reg(node)=no;
reg(node)$(protregValueN(node))=yes;
reg(node)$(merlinValueN(node))=yes;

* turn off ubi - redundant with graph filter
* y.fx(node)$ubiSumoValueN(node)=0;

Set hit(node) "Watanabe hits";
hit(node)=no;
hit(watanabeValueN(node))=yes;
display hit;

Set other(node) "other nodes";
other(node)=yes;
other(node)$(hit(node) or protregValueN(node) or merlinValueN(node))=no;
display other;

* turn off watanabe paths that are not direct
Parameter pathlen(path)	"number of nodes in path";
pathlen(path)=0;	

loop(pnode(node,path), pathlen(path)=pathlen(path)+1; );

* turn off any path with length > 2 edges (3 nodes) - basically,
* allow up to one intermediate between regulators
sigma.fx(path)$(pathlen(path) > 3)=0;

**** Step 0. Turn off Watanabe paths and maximize connections between merlin and protein regs

*sigma.fx(path)$(watanabe_path(path))=0;

Free variable anno	"Included regs and hits";
Equation countAnno "count up included regs and hits";
countAnno .. anno =e= sum(node$(reg(node) or hit(node)), y(node));

Free variable satPairs "connected pairs";
Equation countPairs "count up connected pairs";
countPairs .. satPairs =e= sum( (node,node1)$(merlinpair(node,node1) or protpair(node,node1)), sat(node,node1));

Model maxRegModel /all/ ;
maxRegModel.optcr=0;
maxRegModel.optca=0;
maxRegModel.reslim=100000;
maxRegModel.optfile=1;
* one solution

solve maxRegModel using mip max satPairs;

**** Step 1. Minimize intermediates - no penalty for Watanabe hits

** fix satisfied pairs
Parameter wasSat(node,node1)	"pair satisfied in last solution";
wasSat(node,node1)=no;
wasSat(node,node1)$(merlinpair(node,node1) and sat.l(node,node1)>0)=yes;
wasSat(node,node1)$(protpair(node,node1) and sat.l(node,node1)>0)=yes;
display wasSat;

sat.fx(node,node1)$(wasSat(node,node1))=1;


Equation setBudget "set budget for unannotated nodes";
setBudget .. budget =g= sum(node$other(node), y(node));

Set foundNode(node);
foundNode(node)$(y.l(node) > 0 and (hit(node) or reg(node))) = 1;
y.fx(foundNode)=1;

Model minBudgetModel /all/ ;
minBudgetModel.optcr=0;
minBudgetModel.optca=0;
minBudgetModel.reslim=100000;
minBudgetModel.optfile=2;
* multiple sols

solve minBudgetModel using mip min budget;

Scalar gotBudget "identified budget" /0/;
gotBudget=budget.l;
budget.fx=gotBudget;


**** Step 3. Maximize paths
** For each solution, maximize paths.

* get rid of budget at this point
budget.lo=0;
budget.up=+INF;

Equation countPaths	"count active paths";
Variable pathCount	"count active paths";
countPaths .. pathCount =e= sum(path, sigma(path));

Model maxPathModel /all/ ;
maxPathModel.optcr=0;
maxPathModel.optca=0;
maxPathModel.reslim=10000;
maxPathModel.optfile=1;

set	solnpool(soln) actual solutions;
file fsol;

execute_load 'solnpool.gdx', solnpool=Index;


* get rid of budget at this point
budget.lo=0;
budget.up=+INF;

* keep the nodes per solution
Set solNode(node)	"nodes in a solution";

Scalar count /0/;
loop(solnpool(soln),
	y.up(node)=1;
	y.lo(node)=0;
	
		put_utility fsol 'gdxin' / solnpool.te(soln):0:0;
	putclose;
	execute_loadpoint;
	
	solNode(node)$(y.l(node)>0)=yes;
	
	y.fx(node)=0;
	y.fx(solNode)=1;	
	
	solve maxPathModel using mip max pathCount;
	
	put_utility 'gdxout' / '{OUTPREF}_' count:<0:0 '.gdx';
	execute_unload x,y,sigma,d,sat;

	count=count+1;
);
	
