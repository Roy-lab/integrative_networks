*********
* Model for protein-expression regulator connection for
* influenza analysis.
* V0: Use directed paths. 
* chasman@cs.wisc.edu
* Oct 2014
*********

* don't show input in listing file
$offlisting
* suppress listing and cross-reference of symbols
$offsymxref offsymlist
* equations and vars per block
option
        limrow = 0,
        limcol= 0;


*************Aliases******************

alias (node, node1);
alias (node, node2);


****************Variables*******************
Binary Variables
	x(edge)		edge in use?
	y(node)		node in use?
    d(ppi)		is the edge forward (1) or backward (0) ?
    sigma(path)	is the pathway selected?,
    sat(node,node)	is this pair connected?;
	
Free Variables
	active		count of active edges;
	
*****************Node and edge activity******


******************Equations*******************
Equations	
	countEdge	count active edges
	inUse1(edge,path)	sigma UB by x
	inUse2(edge)	x UB number of active pathways
	nodeOn1(edge,node,node)	is the node selected?
	nodeOn2(edge,node,node)	is the node selected? 
	nodeCount(node)	is the node in any edges
	edgeDirA(edge, path) 	are ppi edges directed properly?
	edgeDirB(edge, path)		are PPI edges directed properly?
	isPairSat(node,node)	is this pair connected by a path?;
		
countEdge ..	active =e= sum(edge, x(edge));

* if edge not in use, sigma must be 0
inUse1(pedge(edge,path)) ..	sigma(path) =l= x(edge);

* edge can only be in use if at least one pathway is
* (or it's a special edge)
inUse2(edge)$(not special(edge))	..	x(edge) =l= sum( pedge(edge,path), sigma(path) );

* If p uses a forward edge and sigma(p)=1, then d(ppi) = 1.
edgeDirA(fwd(ppi,path)) ..	sigma(path) =l= d(ppi);

* If p uses a back edge and sigma(p)=1, then d(ppi) = 1.
edgeDirB(back(ppi,path)) .. sigma(path) =l= 1 - d(ppi);

* edge can't be on without both nodes 
nodeOn1(enode(edge,node,node1)) ..	x(edge) =l= y(node);
nodeOn2(enode(edge,node,node1)) ..	x(edge) =l= y(node1);

* node cannot be on if no edges are on
nodeCount(node) .. y(node) =l= sum( (edge,node1)$(enode(edge,node,node1) or enode(edge,node1,node)), x(edge)); 

 Set dp(node,node)	"directed edges, nodes only";
dp(node,node1)=no;
loop( enode(edge, node, node1)$(not ppi(edge)), dp(node,node1)=yes);

* if both dir and ppi available, deactivate ppi
Set deactivate(ppi);
deactivate(ppi)=no;
loop( enode(ppi, node, node1)$(dp(node,node1) or dp(node1,node)),
	deactivate(ppi)=yes;);
x.fx(deactivate)=0;

* associate paths with pairs
Set pairpath(node, node, path)	"paths associated with pairs of regulators";
pairpath(node,node1,path)=no;
loop((node, node1, path)$(protpair(node, node1) and pnode(node,path) and pnode(node1,path)), pairpath(node,node1,path)=yes;);
loop((node, node1, path)$(merlinpair(node, node1) and pnode(node,path) and pnode(node1,path)), pairpath(node,node1,path)=yes;);

* pair satisfied if at least one path connects them
isPairSat(node,node1) .. sat(node,node1) =l= sum(path$pairpath(node,node1,path), sigma(path));
