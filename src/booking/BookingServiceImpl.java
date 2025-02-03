package booking;

import common.IIRCTCService;
import reservation.IReservationService;
import payment.IPaymentService;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class BookingServiceImpl extends UnicastRemoteObject implements IBookingService {
    private static int bookingCounter = 1;
    private IReservationService reservationService;
    private IPaymentService paymentService;
    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY = 1000; // 1 second

    public BookingServiceImpl() throws RemoteException {
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
                    System.out.println("BookingService successfully connected to dependencies");
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
    public IIRCTCService.BookingDetails bookTicket(int numSeats) throws RemoteException {
        if (reservationService == null || paymentService == null) {
            throw new RemoteException("Services not properly initialized");
        }

        if (!reservationService.checkAvailability(numSeats)) {
            throw new RemoteException("Not enough seats available");
        }

        IIRCTCService.BookingDetails booking = new IIRCTCService.BookingDetails(
            bookingCounter++, numSeats, numSeats * 100.0 // Assuming ₹100 per seat
        );

        if (paymentService.processPayment(booking)) {
            reservationService.updateSeats(numSeats, true);
            booking.status = "CONFIRMED";
            return booking;
        } else {
            booking.status = "FAILED";
            throw new RemoteException("Payment failed");
        }
    }
} 