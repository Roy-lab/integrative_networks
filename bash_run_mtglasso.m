%% Runs MTG-LASSO
% This script should be called from a bash script in order to specify
% input file names/locations on the fly.
% This script requires the SLEP package - you will need to edit this file.

% Intended to be run from bash command -- see infer_protein_regs.sh

% Performs 10-fold CV and gets a single correlation value for 
% all test data for different lambdas.
% Compares each lambda's performance to 40 randomizations of the regulator
% data.
% Requires SLEP!!
addpath(genpath('~/matlab/SLEP_package_4.1/SLEP'));

regulators=importdata(regfile); % protein expression file - specify in bash
regulatorexp=regulators.data;
mids=load(sprintf('%s',mfile));		% mfile must be specified in call to this script

%prints out max threads allowed
maxNumCompThreads 

seed=12345678; % seed randomizer for reproduceability
threshold=0.05; % significance threshold for test against random baseline

if ~exist(outdir)
        mkdir(outdir);
end

lambdas=[0.01, 0.1, 0.25, 0.5, 0.75, 0.99];          % relative range for lambda
numcvs=10;  % number of CV folds
numreps=40;    % random perturbations of regulator data

for m=1:size(mids,1)                                      
%for m=1:1
	fprintf('Module %d: Doing module regression and putting in %s\n',mids(m), outdir);
	targets=importdata(sprintf('%s/c%d_exp.txt',indir,mids(m)));
    
    regweight_fname=sprintf('%s/module%d_regweights.tab', outdir, mids(m));
    
    resultsfname=sprintf('%s/module%d_cv_test_correlation.tab', outdir, mids(m));    
    plotfname=sprintf('%s/module%d_results.tab', outdir, mids(m));
    
    rmsefile=sprintf('%s/module%d_rmse.tab', outdir, mids(m));
    
    % the last thing to be made regardless of success is the RMSE file 
    if exist(rmsefile)
            fprintf('Found rmse file for module %d; skipping.\n', mids(m));
            continue;
    end
    
    % last thing for successful is PDF file.
    pdfname=sprintf('%s/module%d_lasso_all_lambdas.pdf', outdir, mids(m));
    if exist(pdfname)
            fprintf('Found PDF for module %d; skipping.\n', mids(m));
            continue;
    end
       
	[lambda_corrs, lambda_total, rmse_total, lambda_regs, lambda_reg_freqs, lambda_fold_regs] = do_mtglasso_all_lambda(targets,regulators, lambdas, numcvs, regweight_fname);
	
   	% write and plot results      
   	write_per_fold_results(resultsfname, plotfname, lambdas, lambda_corrs, lambda_fold_regs);
    
    X=cell2mat(lambda_corrs);  
    
    fprintf('Doing randomization trials\n');
    
    % random prefix
    rand_prefix=sprintf('%s/module%d', outdir, mids(m));
    % get random values for comparison
    [rand_lambda_totals, rand_lambda_rmse, rand_lambda_reg_freqs] = eval_rand_regs(targets, regulators, lambdas, numreps, numcvs, seed, @do_mtglasso_all_lambda, rand_prefix);
    
    % lambda is success if total cc is significantly GREATER than random
    % means 
    %z-test: for every lambda, compare mean of real correlations to means from random runs 
    successlam=[];  % indices into lambdas
    labels=cell(size(lambdas));
    
    fidrmse=fopen(rmsefile,'w');    % open RMSE file
    for j=1:size(lambdas,2)
        % is corr HIGHER?
        [h,p]=ztest(lambda_total{j}, mean(rand_lambda_totals{j}), std(rand_lambda_totals{j}), 'Tail','Right');
        % is error LOWER?
        [hr,pr]=ztest(rmse_total{j}, mean(rand_lambda_rmse{j}), std(rand_lambda_rmse{j}), 'Tail','Left');
        
        fprintf('Lambda %.2f accuracy compared to random: pearson=%f, rand mu %f, rand SD %f, p=%f\n', lambdas(j), lambda_total{j}, mean(rand_lambda_totals{j}), std(rand_lambda_totals{j}), p);
        fprintf('Lambda %.2f accuracy compared to random: RMSE=%f, rand mu %f, rand SD %f, p=%f\n', lambdas(j), rmse_total{j}, mean(rand_lambda_rmse{j}), std(rand_lambda_rmse{j}), pr);
       
        fprintf(fidrmse, 'Lambda %.2f corr GREATER THAN random: pearson=%f, rand mu %f, rand SD %f, p=%f\n', lambdas(j), lambda_total{j}, mean(rand_lambda_totals{j}), std(rand_lambda_totals{j}), p);
        fprintf(fidrmse, 'Lambda %.2f err LESS THAN random: RMSE=%f, rand mu %f, rand SD %f, p=%f\n', lambdas(j), rmse_total{j}, mean(rand_lambda_rmse{j}), std(rand_lambda_rmse{j}), pr);
         
        
        if (p < threshold || pr < threshold)
            successlam=[ successlam j ];   
            string=''; % star if corr; + if RMSE.
            if p < threshold
                string=sprintf('%s*', string);
            end
            if pr < threshold
                string=sprintf('%s+',string);
            end                           
            labels{j}=sprintf('*%.2f%s', lambda_regs(j), string);
        else
            labels{j}=sprintf('%.2f', lambda_regs(j));
        end
    end   
    fclose(fidrmse);    % close RMSE file
    
    % print out regulators for successful lambdas only
    if size(successlam,1) > 0
        indices=flip(successlam); % order successes biggest to smallest
        chosen=indices(1);  % start with biggest lambda
        
        % print consensus regulators for every good lambda
        for j=1:size(successlam,2)   
            fid=fopen(sprintf('%s/module%d_consensus_regs_lam%.2f.tab', outdir, mids(m), lambdas(indices(j))),'w');       
            
             % print total CC in header, with RMSE and mean regs per fold
            fprintf(fid, '# TOTAL CC\t%.2f\t%.5f\t%.5f\t%.5f\n', lambdas(indices(j)), lambda_total{indices(j)}, rmse_total{indices(j)}, mean(lambda_fold_regs{indices(j)}));
            %fprintf(fid, '# TOTAL CC\t%.2f\t%.5f\n', lambdas(indices(j)), lambda_total{indices(j)});
            fprintf(fid, '# Regulator\tFrequency\tMean_Rand_Freq\tSD_Rand_Freq\tP-val\n');
            allregs=max(lambda_reg_freqs{indices(j)}');
            nz=find(allregs>0);
            
            for s=1:size(nz,2)
                % get random mean, SD, p-value
                random_vals=rand_lambda_reg_freqs{indices(j)}(s,:);
                rand_mu=mean(random_vals);
                rand_sd=std(random_vals);
                [h, p]=ztest(allregs(nz(s)), rand_mu, rand_sd, 'Tail', 'Right');
                fprintf(fid,'%s\t%f\t%f\t%f\t%f\n',regulators.textdata{nz(s)},allregs(nz(s)), rand_mu, rand_sd, p);
            end
            fclose(fid);    
                       
        end 
        fprintf('Module %d: Printed regulators for all good lambda.\n', mids(m));
        
    end
    
    % Plot the correlation values across lambda for real and random trials
    figure;
    hold on;
    
    % plot real values
    plot(cell2mat(lambda_total), '-o', 'LineWidth', 2);
    %boxplot(X, 'labels',labels);
    % plot random distributions; label with number of regulators
    X2=cell2mat(rand_lambda_totals);
    boxplot(X2, 'labels',labels);
    
    title(sprintf('Module %d (MTG-LASSO)', mids(m)));
    xlabel('Avg. regulators per fold (* if corr. above random with p < 0.05)');
    ylabel('Correlation(predicted, test)');
    ylim([-1,1]);
    %legend('real');
    legend({'real'}, 'Position',[0.7,0.8,0.1,0.1]);
    legend('boxoff');
    text(4.9, 0.7, 'boxes: random');
    print(gcf,'-dpdf', '-r300', sprintf('%s/module%d_mtglasso_all_lambdas.pdf', outdir, mids(m)));
    hold off;
  
end


