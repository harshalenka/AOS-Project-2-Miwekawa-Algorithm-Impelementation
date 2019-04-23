@ECHO ON

cd "C:\Users\Yuzuru\git\Project2\Server"
javac -cp ".\bin" -d ".\bin" .\src\*.java

FOR /L %%A IN (1,1,7) DO (
  ECHO %%A
  start "%%A" cmd.exe /k "java -cp ".\bin" Server %%A"
)
cmd /k