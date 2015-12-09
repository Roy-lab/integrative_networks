function write_per_fold_results(resultsfname, plotfname, lambdas, lambda_corrs, lambda_fold_regs)
%% Writes per-fold results to a file.
% Requires: lambda vector; lambda_corrs and lambda_fold_regs cell arrays

% write out module-level correlation for each lambda
% and number of regulators (nonzero)
fid=fopen(resultsfname, 'w');

fprintf(fid,'Fold');
% for each lambda....
for j=1:size(lambdas,2)
   fprintf(fid,'\tcc(lam=%.2f)\tregs(lam=%.2f)', lambdas(j), lambdas(j));
end
fprintf(fid, '\n');


% also print results in plot-friendly format: fold\tx\ty
pid=fopen(plotfname,'w');

% for each fold
for cv=1:size(lambda_corrs{1},1)
    fprintf(fid, 'Fold%d', cv);
    % for each lambda
    for j=1:size(lambdas,2)
        % human-friendly
        fprintf(fid,'\t%.5f\t%d', lambda_corrs{j}(cv), lambda_fold_regs{j}(cv));
        
        % plot-friendly
        fprintf(pid, 'Fold%d\t%d\t%.5f\n', cv, lambda_fold_regs{j}(cv), lambda_corrs{j}(cv));
    end
    fprintf(fid,'\n');      
end
fclose(fid);
fclose(pid);
