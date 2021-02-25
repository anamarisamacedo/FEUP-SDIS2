/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rmi_interface;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 * @author guilh
 */
public interface RMI_Interface extends Remote {
        void backup(String path, int replicationDegree, String senderId) throws RemoteException, Exception;

        void restore(String path, String senderId) throws RemoteException;

        void delete(String path, String senderId) throws RemoteException;

        void reclaim(int spaceToReclaim, String senderId) throws RemoteException;

}
