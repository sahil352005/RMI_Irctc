package server;

import booking.BookingServiceImpl;
import reservation.ReservationServiceImpl;
import payment.PaymentServiceImpl;
import cancellation.CancellationServiceImpl;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;

public class IRCTCServer {
    public static void main(String[] args) {
        try {
            // Create RMI registry
            Registry registry = null;
            try {
                registry = LocateRegistry.createRegistry(1099);
                System.out.println("Java RMI registry created.");
            } catch (RemoteException e) {
                System.out.println("Java RMI registry already exists.");
                registry = LocateRegistry.getRegistry();
            }

            // Create and bind services in the correct order
            ReservationServiceImpl reservationService = new ReservationServiceImpl();
            registry.rebind("ReservationService", reservationService);
            System.out.println("ReservationService bound");

            PaymentServiceImpl paymentService = new PaymentServiceImpl();
            registry.rebind("PaymentService", paymentService);
            System.out.println("PaymentService bound");

            BookingServiceImpl bookingService = new BookingServiceImpl();
            registry.rebind("BookingService", bookingService);
            System.out.println("BookingService bound");

            CancellationServiceImpl cancellationService = new CancellationServiceImpl();
            registry.rebind("CancellationService", cancellationService);
            System.out.println("CancellationService bound");

            System.out.println("IRCTC Server is running on port 1099...");
            
            // Test services
            System.out.println("Testing services...");
            System.out.println("Available seats: " + reservationService.getAvailableSeats());
            System.out.println("All services are working properly!");
            
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
            System.exit(1);
        }
    }
} 