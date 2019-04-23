@ECHO ON

cd "C:\Users\Yuzuru\Desktop\Project_2\Client"
javac -cp ".\bin" -d ".\bin" .\src\*.java

FOR /L %%A IN (11,1,15) DO (
  ECHO %%A
  start "%%A" cmd.exe /k "java -cp ".\bin" Client %%A"
)
cmd /k