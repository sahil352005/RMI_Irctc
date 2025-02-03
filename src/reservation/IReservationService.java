package reservation;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IReservationService extends Remote {
    boolean checkAvailability(int numSeats) throws RemoteException;
    void updateSeats(int numSeats, boolean isBooking) throws RemoteException;
    int getAvailableSeats() throws RemoteException;
} 