MAIN_OBJECTS_COMMUNICATION_CHANNELS =  communicationChannels/*.java communicationChannels/client/*.java communicationChannels/centralizedChordManager/actions/*.java communicationChannels/centralizedChordManager/server/*.java communicationChannels/peer/actions/*.java communicationChannels/peer/server/*.java
MAIN_OBJECTS_MODELS = models/*.java
MAIN_OBJECTS_PROTOCOL_ACTIONS = protocolActions/*.java
MAIN_OBJECTS_RMI_INTERFACE = rmi_interface/*.java
MAIN_OBJECTS_UTILS = utils/*.java
MAIN_OBJECTS_THREADS = threads/*.java
TEST_APP = TestApp.java
		

JAVAC = javac

all: build_communication_channels build_rmi_interface build_utils build_models build_threads build_test_app

build_communication_channels: $(MAIN_OBJECTS_COMMUNICATION_CHANNELS)
	$(JAVAC)  $(MAIN_OBJECTS_COMMUNICATION_CHANNELS) 

build_models: $(MAIN_OBJECTS_MODELS)
	$(JAVAC) $(MAIN_OBJECTS_MODELS)

build_rmi_interface: $(MAIN_OBJECTS_RMI_INTERFACE)
	$(JAVAC) $(MAIN_OBJECTS_RMI_INTERFACE)

build_utils: $(MAIN_OBJECTS_UTILS)
	$(JAVAC) $(MAIN_OBJECTS_UTILS)

build_threads: $(MAIN_OBJECTS_THREADS)
	$(JAVAC) $(MAIN_OBJECTS_THREADS)

build_test_app: $(TEST_APP)
	$(JAVAC) $(TEST_APP)
		