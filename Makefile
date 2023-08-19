TMPFILE = git-commit-status-message

deploy: clean add commit push

clean:
	rm -f ${TMPFILE}

add:
	git add .

commit:
	touch ${TMPFILE}
	git status --porcelain \
		| grep '^[MARCDT]' \
		| sort \
		| sed -E 's/^([[:upper:]])[[:upper:]]?[[:space:]]+/\\1:\\n\\ /' \
		| awk '!x[$$0]++' \
		| sed -E 's/^([[:upper:]]:)$$/\\n\\1/' \
		| sed -E 's/^M:$$/Modified: /' \
		| sed -E 's/^A:$$/Added: /' \
		| sed -E 's/^R:$$/Renamed: /' \
		| sed -E 's/^C:$$/Copied: /' \
		| sed -E 's/^D:$$/Deleted: /' \
		| sed -E 's/^T:$$/File Type Changed: /' \
		| tr '\n' ' ' | xargs \
		> ${TMPFILE}
	git commit -F ${TMPFILE}
	rm -f ${TMPFILE}

push:
	git remote | xargs -L1 git push --all


cp:
	git add .
	@read -p "Commit message: " m; \
	git commit -m "$$m"
	make push