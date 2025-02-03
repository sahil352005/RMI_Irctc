package cancellation;

import common.IIRCTCService;
import reservation.IReservationService;
import payment.IPaymentService;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class CancellationServiceImpl extends UnicastRemoteObject implements ICancellationService {
    private IReservationService reservationService;
    private IPaymentService paymentService;
    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY = 1000; // 1 second

    public CancellationServiceImpl() throws RemoteException {
        super();
        initializeServices();
    }

    private void initializeServices() {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                reservationService = (IReservationService) registry.lookup("ReservationService");
                paymentService = (IPaymentService) registry.lookup("PaymentService");
                
                if (reservationService != null && paymentService != null) {
                    System.out.println("CancellationService successfully connected to dependencies");
                    return;
                }
            } catch (Exception e) {
                System.err.println("Attempt " + (i + 1) + " to connect to services failed");
                try {
                    Thread.sleep(RETRY_DELAY);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        System.err.println("Failed to initialize services after " + MAX_RETRIES + " attempts");
    }

    @Override
    public boolean cancelBooking(IIRCTCService.BookingDetails booking) throws RemoteException {
        System.out.println("CancellationService: Attempting to cancel booking " + booking.bookingId);
        
        if (reservationService == null || paymentService == null) {
            System.out.println("CancellationService: Services not initialized");
            throw new RemoteException("Services not properly initialized");
        }

        try {
            if (booking.status.equals("CONFIRMED")) {
                System.out.println("CancellationService: Processing refund");
                if (paymentService.processRefund(booking)) {
                    System.out.println("CancellationService: Refund processed, updating seats");
                    reservationService.updateSeats(booking.numSeats, false);
                    booking.status = "CANCELLED";
                    System.out.println("CancellationService: Booking cancelled successfully");
                    return true;
                }
            }
            System.out.println("CancellationService: Cancellation failed - booking not confirmed or refund failed");
            return false;
        } catch (Exception e) {
            System.out.println("CancellationService: Error during cancellation - " + e.getMessage());
            e.printStackTrace();
            throw new RemoteException("Cancellation failed: " + e.getMessage());
        }
    }
} 