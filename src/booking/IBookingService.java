package booking;

import common.IIRCTCService;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IBookingService extends Remote {
    IIRCTCService.BookingDetails bookTicket(int numSeats) throws RemoteException;
} 