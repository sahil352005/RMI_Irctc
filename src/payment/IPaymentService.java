package payment;

import common.IIRCTCService;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IPaymentService extends Remote {
    boolean processPayment(IIRCTCService.BookingDetails booking) throws RemoteException;
    boolean processRefund(IIRCTCService.BookingDetails booking) throws RemoteException;
} 