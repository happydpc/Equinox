'''
Created on 26 Sep 2018

@author: aurora
'''

import socket

# inputs: port number and path to instruction set
portNumber = 1789
inputPath = "/Users/aurora/EclipseWorkspace/Equinox/docs/plugin_api/NewFile.xml"

# create network socket and connect to automation server
s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect(('localhost', portNumber))

# send input file path to server and close socket
s.send(inputPath.encode("utf8"))
s.close() 