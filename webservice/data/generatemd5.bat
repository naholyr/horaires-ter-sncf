@ECHO OFF
CD %~dp0
php -r "echo md5(file_get_contents('gares.utf8.txt'));" > gares.utf8.txt.md5
pause