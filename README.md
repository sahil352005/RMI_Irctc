RMI Implementation in Irctc Website
steps:
step 1:make bin folder
mkdir bin
step 2 : # Compile all files
javac -d bin -cp bin src/common/*.java src/reservation/*.java src/payment/*.java src/booking/*.java src/cancellation/*.java src/gui/*.java src/server/*.java src/client/*.java
step 3: Run server
cd bin
java server.IRCTCServer
step 4: In a new terminal to run client
cd bin
java client.IRCTCClient
