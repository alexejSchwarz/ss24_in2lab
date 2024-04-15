package com.haw.srs.customerservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
public class CourseService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private MailGateway mailGateway;


    @Transactional
    public void enrollInCourse(String lastName, Course course) throws CustomerNotFoundException {
        Customer customer = customerRepository
                .findByLastName(lastName)
                .orElseThrow(() -> new CustomerNotFoundException(lastName));

        customer.addCourse(course);
        // TODO muss course im repo gespeichert werden? Wann muessen Entities gespeichert werden?
        incCourseMembNumb(course);
        customerRepository.save(customer);
    }

    @Transactional
    public void transferCourses(String fromCustomerLastName, String toCustomerLastName) throws CustomerNotFoundException {
        Customer from = customerRepository
                .findByLastName(fromCustomerLastName)
                .orElseThrow(() -> new CustomerNotFoundException(fromCustomerLastName));
        Customer to = customerRepository
                .findByLastName(toCustomerLastName)
                .orElseThrow(() -> new CustomerNotFoundException(toCustomerLastName));

        // TODO Kurse die from hat und to nicht hat memb +-0.
        //  from true, to true -> course memb -1
        to.getCourses().addAll(from.getCourses());
        from.getCourses().clear();

        customerRepository.save(from);
        customerRepository.save(to);
    }

    /**
     * Cancels a course membership. An Email is sent to all possible participants on the waiting list for this course.
     * If customer is not member of the provided course, the operation is ignored.
     *
     * @throws IllegalArgumentException if customerNumber==null or courseNumber==null
     */
    @Transactional
    public void cancelMembership(CustomerNumber customerNumber, CourseNumber courseNumber) throws CustomerNotFoundException, CourseNotFoundException, MembershipMailNotSent {

        // some implementation goes here
        // find customer, find course, look for membership, remove membership, etc.
        // TODO customer finden, cust und course speichern? E-mail?
        // TODO ist NP in Ordnung oder muss unbedingt IllegalArgument sein?
        // TODO inwie fern ignorieren?

        Objects.requireNonNull(customerNumber);
        Objects.requireNonNull(courseNumber);

        String customerMail = "customer@domain.com";
        Optional<Course> optC = courseRepository
                .findById(courseNumber.getCourseNumber());

        if (optC.isEmpty()) {
            return;
        }
        Course c = optC.get();

        decCourseMembNumb(c);

        boolean mailWasSent = mailGateway.sendMail(customerMail, "Oh, we're sorry that you canceled your membership!", "Some text to make her/him come back again...");
        if (!mailWasSent) {
            // do some error handling here (including e.g. transaction rollback, etc.)
            // ...
            
            throw new MembershipMailNotSent(customerMail);
        }
    }

    // TODO course speichern hier besser, wenn noetig
    private void incCourseMembNumb(Course c) {
        int old = c.getAnzahlTeilnehmer();
        c.setAnzahlTeilnehmer(old + 1);
        courseRepository.save(c);
    }

    private void decCourseMembNumb(Course c) {
        int old = c.getAnzahlTeilnehmer();
        c.setAnzahlTeilnehmer(old - 1);
        courseRepository.save(c);
    }
}
