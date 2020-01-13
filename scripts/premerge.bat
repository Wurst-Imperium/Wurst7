@echo off
cd ..
set /p id=Pull Request #
set branch=pr%id%
call git fetch origin pull/%id%/head:%branch%
call git checkout %branch%
pause