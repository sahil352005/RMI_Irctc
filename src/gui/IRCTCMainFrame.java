package gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import javax.swing.event.ListSelectionListener;
import java.awt.event.*;
import common.IIRCTCService;
import booking.IBookingService;
import reservation.IReservationService;
import cancellation.ICancellationService;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.List;

public class IRCTCMainFrame extends JFrame {
    private IBookingService bookingService;
    private IReservationService reservationService;
    private ICancellationService cancellationService;
    private JLabel availableSeatsLabel;
    private JTextField numTicketsField;
    private List<IIRCTCService.BookingDetails> bookingHistory;
    private JPanel cardPanel;
    private CardLayout cardLayout;
    private JTable bookingHistoryTable;

    public IRCTCMainFrame() {
        super("IRCTC Ticket Booking System");
        bookingHistory = new ArrayList<>();
        initializeServices();
        setupGUI();
    }

    private void initializeServices() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            bookingService = (IBookingService) registry.lookup("BookingService");
            reservationService = (IReservationService) registry.lookup("ReservationService");
            cancellationService = (ICancellationService) registry.lookup("CancellationService");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error connecting to services: " + e.getMessage());
            System.exit(1);
        }
    }

    private void setupGUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 500);
        setLocationRelativeTo(null);

        // Create menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Menu");
        
        JMenuItem bookingItem = new JMenuItem("Book Tickets");
        JMenuItem historyItem = new JMenuItem("Booking History");
        
        bookingItem.addActionListener(e -> showCard("booking"));
        historyItem.addActionListener(e -> {
            updateBookingHistoryPanel();
            showCard("history");
        });
        
        menu.add(bookingItem);
        menu.add(historyItem);
        menuBar.add(menu);
        setJMenuBar(menuBar);

        // Create card layout
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        // Add booking panel
        cardPanel.add(createBookingPanel(), "booking");
        
        // Add history panel
        cardPanel.add(createBookingHistoryPanel(), "history");

        add(cardPanel);
        cardLayout.show(cardPanel, "booking");
    }

    private JPanel createBookingPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Title
        JLabel titleLabel = new JLabel("IRCTC Booking System", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));

        // Available Seats
        try {
            availableSeatsLabel = new JLabel("Available Seats: " + 
                reservationService.getAvailableSeats(), SwingConstants.CENTER);
        } catch (RemoteException e) {
            availableSeatsLabel = new JLabel("Error fetching seats", SwingConstants.CENTER);
        }
        availableSeatsLabel.setFont(new Font("Arial", Font.PLAIN, 18));

        // Booking Panel
        JPanel bookingInputPanel = new JPanel(new FlowLayout());
        bookingInputPanel.add(new JLabel("Number of Tickets:"));
        numTicketsField = new JTextField(5);
        bookingInputPanel.add(numTicketsField);

        // Book Button
        JButton bookButton = new JButton("Book Tickets");
        bookButton.addActionListener(e -> bookTickets());

        // Add components
        panel.add(titleLabel);
        panel.add(availableSeatsLabel);
        panel.add(bookingInputPanel);
        panel.add(bookButton);

        return panel;
    }

    private JPanel createBookingHistoryPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Title Panel
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel titleLabel = new JLabel("Booking History", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titlePanel.add(titleLabel);
        mainPanel.add(titlePanel, BorderLayout.NORTH);

        // Table Panel
        JPanel tablePanel = new JPanel(new BorderLayout());
        String[] columnNames = {"Booking ID", "Seats", "Amount", "Status"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
        };
        bookingHistoryTable = new JTable(model);
        bookingHistoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bookingHistoryTable.setRowHeight(25);
        
        // Set column widths
        TableColumnModel columnModel = bookingHistoryTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(100); // Booking ID
        columnModel.getColumn(1).setPreferredWidth(80);  // Seats
        columnModel.getColumn(2).setPreferredWidth(100); // Amount
        columnModel.getColumn(3).setPreferredWidth(100); // Status

        JScrollPane scrollPane = new JScrollPane(bookingHistoryTable);
        scrollPane.setPreferredSize(new Dimension(500, 300));
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(tablePanel, BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        JButton cancelButton = new JButton("Cancel Selected Booking");
        cancelButton.setPreferredSize(new Dimension(200, 30));
        cancelButton.setFont(new Font("Arial", Font.BOLD, 14));
        cancelButton.addActionListener(e -> {
            int selectedRow = bookingHistoryTable.getSelectedRow();
            if (selectedRow != -1) {
                IIRCTCService.BookingDetails booking = bookingHistory.get(selectedRow);
                if (booking.status.equals("CONFIRMED")) {
                    cancelBooking(booking);
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Only CONFIRMED bookings can be cancelled",
                        "Cannot Cancel", JOptionPane.WARNING_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this,
                    "Please select a booking to cancel",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            }
        });
        buttonPanel.add(cancelButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Refresh button
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> updateBookingHistoryPanel());
        buttonPanel.add(refreshButton);

        return mainPanel;
    }

    private void updateBookingHistoryPanel() {
        DefaultTableModel model = (DefaultTableModel) bookingHistoryTable.getModel();
        model.setRowCount(0); // Clear existing rows

        for (IIRCTCService.BookingDetails booking : bookingHistory) {
            model.addRow(new Object[]{
                booking.bookingId,
                booking.numSeats,
                "₹" + booking.amount,
                booking.status
            });
        }
        
        // Select first row if available
        if (model.getRowCount() > 0) {
            bookingHistoryTable.setRowSelectionInterval(0, 0);
        }
    }

    private void showCard(String cardName) {
        cardLayout.show(cardPanel, cardName);
    }

    private void bookTickets() {
        try {
            int numTickets = Integer.parseInt(numTicketsField.getText());
            if (numTickets <= 0) {
                throw new NumberFormatException();
            }
            
            IIRCTCService.BookingDetails booking = bookingService.bookTicket(numTickets);
            bookingHistory.add(booking);
            
            if (booking.status.equals("CONFIRMED")) {
                JOptionPane.showMessageDialog(this, 
                    "Booking Successful!\nBooking ID: " + booking.bookingId +
                    "\nAmount: ₹" + booking.amount);
                availableSeatsLabel.setText("Available Seats: " + 
                    reservationService.getAvailableSeats());
                numTicketsField.setText("");
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Booking failed", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, 
                "Please enter a valid number of tickets", 
                "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Booking failed: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelBooking(IIRCTCService.BookingDetails booking) {
        try {
            System.out.println("Attempting to cancel booking: " + booking.bookingId);
            boolean cancelled = cancellationService.cancelBooking(booking);
            if (cancelled) {
                booking.status = "CANCELLED"; // Update the status locally
                JOptionPane.showMessageDialog(this, 
                    "Booking cancelled successfully!\nRefund amount: ₹" + booking.amount);
                availableSeatsLabel.setText("Available Seats: " + 
                    reservationService.getAvailableSeats());
                updateBookingHistoryPanel();
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Cancellation failed", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace(); // Add this for debugging
            JOptionPane.showMessageDialog(this, 
                "Cancellation failed: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new IRCTCMainFrame().setVisible(true);
        });
    }
} 