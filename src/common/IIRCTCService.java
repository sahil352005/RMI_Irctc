package common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IIRCTCService extends Remote {
    // Common data structure for booking details
    public static class BookingDetails implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        public int bookingId;
        public int numSeats;
        public double amount;
        public String status;
        
        public BookingDetails(int bookingId, int numSeats, double amount) {
            this.bookingId = bookingId;
            this.numSeats = numSeats;
            this.amount = amount;
            this.status = "PENDING";
        }
    }
} 