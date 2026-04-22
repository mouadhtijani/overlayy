# merge la branche pre-release dans integration
git checkout release/integration && git pull && git merge --no-edit -X theirs release/pre-release && git push && git checkout release/pre-release