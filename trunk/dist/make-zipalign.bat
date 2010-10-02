copy "Horaires-TER-SNCF.apk" "Horaires-TER-SNCF-unaligned.apk"
C:\Users\naholyr\Outils\Android\SDK_r07\tools\zipalign.exe -f -v 4 "Horaires-TER-SNCF-unaligned.apk" "Horaires-TER-SNCF-aligned.apk"
C:\Users\naholyr\Outils\Android\SDK_r07\tools\zipalign.exe -c -v 4 "Horaires-TER-SNCF-aligned.apk"
del "Horaires-TER-SNCF-unaligned.apk"

