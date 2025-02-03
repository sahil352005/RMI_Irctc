package cancellation;

import common.IIRCTCService;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ICancellationService extends Remote {
    boolean cancelBooking(IIRCTCService.BookingDetails booking) throws RemoteException;
} 