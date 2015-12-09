function [ rand_lambda_totals, rand_lambda_total_rmse, rand_lambda_reg_freqs ] = eval_rand_regs(targets, regulators, lambdas, numreps, numcvs, seed, optfun, out_prefix)
%% Applies the 'optfun' function (MTGLasso or Lasso) to num_reps randomized regulator data sets.
% Returns rand_lambda_totals, which is a cell array indexed over lambda's
% indices and contains numreps correlation values for each lambda. 
% (one value for the whole test set, for each random iteration)
% out_prefix gives filename prefix for regression weights 
% (like "results/module1549")
% V2: Reports RMSE AND correlation values.

% Each cell contains a matrix with numreps rows and numcvs columns.

% all correlation values for all folds for each replicate
rand_lambda_corrs=cell(size(lambdas));

% total cc value across all folds for each replicate
rand_lambda_totals=cell(size(lambdas));
rand_lambda_total_rmse=cell(size(lambdas));

% save regulator frequencies across random runs
rand_lambda_reg_freqs=cell(size(lambdas));

for j=1:size(rand_lambda_corrs,2)
   rand_lambda_corrs{j}=zeros(numcvs, numreps);
   rand_lambda_totals{j}=zeros(numreps,1);
   
   % one value per regulator per rep
   rand_lambda_reg_freqs{j}=zeros(size(regulators.data,1), numreps);
end

% seed rng for reproducible results
rng(seed);

% for each repeat
for r=1:numreps
   % get permutation of sample IDs 
   p = randperm(size(regulators.data,2));   
   
   % copy regulator data
   randregs=regulators;
   
   % permute columns of data
   randregs.data=regulators.data(:,p);
   
   randfn=sprintf('%s_rand%d_regweights.tab', out_prefix, r);
   
   % do method on permuted regs - get FDR for random regulators   
   [ lambda_corrs, lambda_total, rmse_total, lambda_regs, lambda_reg_freqs, lambda_fold_regs ] = optfun(targets,randregs, lambdas, numcvs, randfn);
   
   % save correlation values and regulator frequencies
   for j=1:size(lambdas,2)
        rand_lambda_totals{j}(r,:)=lambda_total{j};
        rand_lambda_total_rmse{j}(r,:)=rmse_total{j};
    
        % regulator frequency values
        allregs=max(lambda_reg_freqs{j}');
        nz=find(allregs>0);
        for s=1:size(nz,2)
            rand_lambda_reg_freqs{j}(nz(s), r)=allregs(nz(s));            
        end               
   end
      
   
   if mod(r,10)==0
       fprintf('Done with %d random trials\n', r);
   end	
end


