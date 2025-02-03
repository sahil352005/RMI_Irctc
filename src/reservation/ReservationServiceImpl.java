package reservation;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ReservationServiceImpl extends UnicastRemoteObject implements IReservationService {
    private static final int TOTAL_SEATS = 100;
    private int availableSeats;

    public ReservationServiceImpl() throws RemoteException {
        super();
        this.availableSeats = TOTAL_SEATS;
    }

    @Override
    public synchronized boolean checkAvailability(int numSeats) throws RemoteException {
        return availableSeats >= numSeats;
    }

    @Override
    public synchronized void updateSeats(int numSeats, boolean isBooking) throws RemoteException {
        if (isBooking) {
            availableSeats -= numSeats;
        } else {
            availableSeats += numSeats;
        }
    }

    @Override
    public int getAvailableSeats() throws RemoteException {
        return availableSeats;
    }
} 