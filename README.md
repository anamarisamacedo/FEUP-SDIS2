# sdis1920-t5g01

# Running instructions

- Make sure you have JDK and JRE installed [Link](https://docs.oracle.com/javase/8/docs/technotes/guides/install/install_overview.html)
- Navigate to the src folder
- Compile the java files running `make`
- JDK version used: `jdk-13.0.2`

### Server

_As fault tolerance does not work properly please restart the procedure starting at chordServer to run peers and chordServer_

#### ChordServer

- At the src folder:
- To run the chordServer run `java models.CentralizedChordManager chordServer_port chordServer_ip maxPeerNumbers` eg `java models.CentralizedChordManager 8000 localhost 10` :
  - `chord_port` port of chord server (sslEngine) e.g `8000`
  - `chordServer_ip`: IP of chord server e.g `localhost`
  - `maxPeerNumbers` max number of peers allowed on the chord network eg `10`

#### Peer

- At the src folder:
- Run `rmiregistry`
- To run a server run `java models.Peer version id peer_ap sslEngine_port chordServer_ip chordServer_port maxPeerNumbers peer_ip` e.g `java models.Peer 1.0 1 Peer1 8001 localhost 8000 10 localhost` :
  - `version` : version of server to use e.g `1.0`
  - `id` : id of peer e.g `1`
  - `peer_ap` : access point of peer e.g `Peer1`
  - `sslEngine_port` : port of this peer (sslEngine) eg `8001`
  - `chordServer_ip`: IP of chord server e.g `localhost`
  - `chord_port` port of chord server (sslEngine) e.g `8000`
  - `maxPeerNumbers` max number of peers allowed on the chord network eg `10`
  - `peer_ip` : IP of this peer e.g `localhost`
- Run a peer at a time and wait for the message: `Peer with id of 1 created` before initiating another peer
- Run `ctrl+c` to properly save peer.bin on exit

### Client

- To run a client run `java TestApp <peer_ap> <operation> <opnd_1> <opnd_2>` e.g `java TestApp Peer1 BACKUP files/Doge.jpg 2` (Starts a backup service on peer 1 of file files/Doge.jpg with a replication degree of 2)
  - <peer_ap> Is the peer's access point. This depends on the implementation. (See the previous section) e.g `Peer1`
  - <operation> Is the operation the peer of the backup service must execute. It can be either the triggering of the subprotocol to test, or the retrieval of the peer's internal state. In the first case it must be one of: BACKUP, RESTORE, DELETE, RECLAIM. To retrieve the internal state, the value of this argument must be STATE e.g `BACKUP`
  - <opnd_1> Is either the path name of the file to backup/restore/delete, for the respective 3 subprotocols, or, in the case of RECLAIM the maximum amount of disk space (in KByte) that the service can use to store the chunks. In the latter case, the peer should execute the RECLAIM protocol, upon deletion of any chunk. The STATE operation takes no operands. e.g `files/Doge.jpg`
  - <opnd_2> This operand is an integer that specifies the desired replication degree and applies only to the backup protocol (or its enhancement) e.g `2`

# File structure

- In the `src` folder `tmp` folder will be created after a peer has been created.
- `Peerx` folder corresponds to the folder of a peer with its Id replacing x
- `data` folder is where peer.bin is saved
- `files` folder is where all file info received during BACKUP, RESTORE, RECLAIM and DELETE is saved
- `fileId` folder is where info about that file is saved
- `storage` folder is where files
- `restored` is the folder where restored files are saved
- `filePath` is the original filePath of the restored file

```
tmp
└───Peerx
    │   data
    │   └───Peerx.bin
    └───files
        └───storage
        │   └───fileId
        │
        └───restored
            └───filePath

```
