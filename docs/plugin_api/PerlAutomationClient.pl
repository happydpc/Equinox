use IO::Socket;

# inputs: host name, port number and path to instruction set
$remote_host = "localhost";
$remote_port = 1789;
$input_path = "/Users/aurora/EclipseWorkspace/Equinox/docs/plugin_api/NewFile.xml";

# create network socket and connect to automation server
$socket = IO::Socket::INET->new(PeerAddr => $remote_host, PeerPort => $remote_port, Proto => "tcp", Type => SOCK_STREAM)
    or die "Couldn't connect to $remote_host:$remote_port : $@\n";

# send input file path to server and close socket
print $socket "$input_path\n";
close($socket);