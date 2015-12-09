function [lambda_corrs, lambda_total, rmse_total, lambda_regs, lambda_reg_freqs, lambda_fold_regs] = do_lasso_all_lambda(targets, regulators, lambdas, numcvs, regweight_fname)
%% Runs regular lasso to infer protein regulators for each target gene individually.
% V2: Reports both RMSE and CC for all module genes.

targetexp=targets.data;
regulatorexp=regulators.data;

q=2;

% normalize data
mt=mean(targetexp,2);
mr=mean(regulatorexp,2);
targetexp_norm=targetexp-repmat(mt,1,size(targetexp,2));
regulatorexp_norm=regulatorexp-repmat(mr,1,size(regulatorexp,2));

numsamples=size(targetexp,2);
indices=1:numsamples;

newtargetexp=targetexp_norm(:,indices);
newregulatorexp=regulatorexp_norm(:,indices);
cvsize=floor(numsamples/numcvs);


% save regulator counts per gene across CV rows are regs, cols are targets.
% one per lambda.
reg_counts=cell(size(lambdas));

% save number of consensus regulators per lambda
lambda_regs=zeros(size(lambdas));


% collect module-level correlation for all lambdas
% each item in lambdas is a vector of values (size of folds)
lambda_corrs=cell(size(lambdas));  

% save test prediction values for each lambda
lambda_predictions=cell(size(lambdas));

% total CC and RMSE
lambda_total=cell(size(lambdas));
rmse_total=cell(size(lambdas));

lambda_reg_freqs=cell(size(lambdas));

% tally up regulators per fold 
lambda_fold_tally=cell(size(lambdas));
% and count average numer per fold
lambda_fold_regs=cell(size(lambdas));

% print regression weights per fold, per lambda 
pid=fopen(regweight_fname,'w');

for j=1:size(lambdas,2)
    lambda_corrs{j}=zeros(numcvs,1);
    lambda_fold_tally{j}=zeros(numcvs, size(regulatorexp,1));
    lambda_fold_regs{j}=zeros(numcvs, 1);
    
    reg_counts{j}=zeros(size(regulatorexp,1), size(targetexp,1));
    lambda_predictions{j}=[];
    lambda_total{j}=0;
    rmse_total{j}=0;
    lambda_reg_freqs{j}=zeros(size(regulatorexp,1), size(targetexp,1));
end


for cv=1:numcvs                            
	%fprintf('Doing CV %d\n',cv);
    % identify train/test sample indices
    % move test start along the indices
	testbegin=((cv-1)*cvsize)+1;
	if(cv<numcvs)
		testend=cv*cvsize;
	else
		testend=numsamples;
	end
	trainind=setdiff(1:numsamples,testbegin:testend);
	if(isempty(trainind))
		trainind=1:numsamples;
    end
    
      
	%Regulators are on the rows and experiments are on the columns
	%X is for regulators and Y is for targets
	testX=newregulatorexp(:,testbegin:testend);
	testY=newtargetexp(:,testbegin:testend);
    
	trainX=newregulatorexp(:,trainind);
	trainY=newtargetexp(:,trainind);
    
	% number of module genes = rows in target matrix
	genect=size(newtargetexp,1);      
                               
    % predicted expression values for all genes
    % cell array is indexed by lambda
    % each cell contains a matrix the size of testY
    allpred=cell(size(lambdas));
    for j=1:size(allpred,2)
            allpred{j}=zeros(size(testY));
    end          
        
    % save all the X for every lambda
    all_X=cell(size(lambdas));                   
    
    % now do each target gene separately
	for c=1:genect               
        % select the row in Y - the target gene
		Y=trainY(c,:)';        
        % A is regulator data matrix - use the whole thing
		A=trainX';        
    	Atest=testX';
		Ytest=testY(c,:);
	
        
        opts=[];
        opts.init=2;
        opts.tFlag=5;
        opts.maxIter=100;   % maximum number of iterations
        opts.nFlag=0;       % without normalization
        opts.rFlag=1;       % choose max lambda and let us transit the full space
        %opts.rho=0.0001;    % negligible impact of rho - leave empty OK?
        opts.mFlag=0;       % treating it as compositive function 
        opts.lFlag=0;       % Nemirovski's line search
        opts.fName='LeastR';
        tic;
        % do pathwise solution search for range of lambdas
        X = pathSolutionLeast(A,Y,lambdas, opts);  

        % save X
        all_X{c}=X;           

        % save predictions from each lambda        
        for j=1:size(X,2)
            x1=X(:,j);
            cpred=Atest*x1;
            actual=Ytest;
            cc1=corrcoef(cpred,actual);                           
            % save prediction for this gene, this lambda, all conditions
            allpred{j}(c,:)=cpred';    
            
            % TEST
            % x1' gives coefficients for each regulator and this gene
            % it's a matrix over regs and targets.
            for ix=1:size(x1,1)
                if x1(ix)~=0
                        % lambda, fold, reg, target, weight
                        fprintf(pid, '%f\t%d\t%s\t%s\t%f\n', lambdas(j), cv, regulators.textdata{ix}, targets.textdata{c}, x1(ix));
                end
            end
            
            % get regulators for this gene        
            % x1' gives total coefficients for each regulator
            s=sum(x1');
            % get coeffs that are nonzero - linear indices into x1 matrix
            nzb=find(x1~=0);   
            x2=zeros(size(x1));
            % set 
            x2(nzb)=1;  % mark non-zero coeffs with 1s

            % add to reg-target counts
            reg_counts{j}(:,c)=reg_counts{j}(:,c)+x2;        
            lambda_fold_tally{j}(cv,:)=lambda_fold_tally{j}(cv,:)+x2'; 
        end 
    end

    % another loop over lambdas for this fold to get module-level
    % correlation and RMSE for all genes in this fold
    for j=1:size(allpred,2)
        lampred=allpred{j};
        % flattens the two matrices into vectors
        cc=corrcoef(lampred(:),testY(:));
        %fprintf('Test correlations modulelevel lambda=%.2f cc=%.2f\n',lambdas(j), cc(1,2));
        
        % save value for lambda, fold
        lambda_corrs{j}(cv) =  cc(1,2);    
            
        % save predictions across all folds
        lambda_predictions{j}=[ lambda_predictions{j} lampred ];        
        
    end   
end

for j=1:size(lambdas,2)
    % get total CC
    cc= corrcoef(lambda_predictions{j}(:), newtargetexp(:));
    lambda_total{j}=cc(1,2);
    
    % calculate RMSE per target
    rmse=sqrt(mean((lambda_predictions{j}(:) - newtargetexp(:)).^2));
    rmse_total{j}=rmse;
    
    % get regulator frequency
    lambda_reg_freqs{j}=reg_counts{j}/numcvs;

    % save mean regs per fold
    foldcts=[];
    for cv=1:numcvs
        % count up nonzero regulators for this fold
        foldregs=size(find(lambda_fold_tally{j}(cv,:) ~= 0),2);
        lambda_fold_regs{j}(cv)=foldregs;        
    end
    lambda_regs(j)=mean(lambda_fold_regs{j}'); %         %size(nzregs,1);
    
end

