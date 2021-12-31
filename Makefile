# example:
# $make git m="commit message"
# use store to permanently store your token and cache for temporary store (15 mins)

git:
	git add -A
	git commit -m "$m"
	git push

store:
	git config --global credential.helper store

cache:
	git config --global credential.helper cache