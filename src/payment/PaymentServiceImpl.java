package payment;

import common.IIRCTCService;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class PaymentServiceImpl extends UnicastRemoteObject implements IPaymentService {
    
    public PaymentServiceImpl() throws RemoteException {
        super();
    }

    @Override
    public boolean processPayment(IIRCTCService.BookingDetails booking) throws RemoteException {
        // Simulate payment processing
        return Math.random() > 0.1; // 90% success rate
    }

    @Override
    public boolean processRefund(IIRCTCService.BookingDetails booking) throws RemoteException {
        // Simulate refund processing
        return Math.random() > 0.05; // 95% success rate
    }
} 