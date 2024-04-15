package com.haw.srs.customerservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class CourseServiceTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CourseRepository courseRepository;

    @MockBean
    private MailGateway mailGateway;

    private Customer testCust;

    private Course testCourse;

    @BeforeEach
    void setup() {
        customerRepository.deleteAll();
        courseRepository.deleteAll();
        testCust = new Customer("Jane", "Doe", Gender.FEMALE, "jane.doe@mail.com", null);
        testCourse = new Course("Software Engineering 1");
    }

    @Test
    void enrollCustomerInCourseSuccess() throws CustomerNotFoundException {
        customerRepository.save(testCust);

        assertThat(testCust.getCourses()).size().isEqualTo(0);

        assertEquals(0, testCourse.getAnzahlTeilnehmer());
        courseService.enrollInCourse(testCust.getLastName(), testCourse);

        assertThat(customerService.findCustomerByLastname(testCust.getLastName()).getCourses())
                .size().isEqualTo(1);
        assertEquals(1, testCourse.getAnzahlTeilnehmer());
    }

    @Test
    void enrollCustomerInCourseFailBecauseOfCustomerNotFound() {
        assertThatExceptionOfType(CustomerNotFoundException.class)
                .isThrownBy(() -> courseService.enrollInCourse("notExisting", testCourse))
                .withMessageContaining("Could not find customer with lastname notExisting.");
    }

    @Test
    void transferCoursesSuccess() throws CustomerNotFoundException {
        Customer from = new Customer("John", "Smith", Gender.MALE);
        from.addCourse(testCourse);
        from.addCourse(new Course("Software Engineering 2"));
        customerRepository.save(from);
        Customer to = new Customer("Eva", "Miller", Gender.FEMALE);
        customerRepository.save(to);

        assertThat(from.getCourses()).size().isEqualTo(2);
        assertThat(to.getCourses()).size().isEqualTo(0);

        courseService.transferCourses(from.getLastName(), to.getLastName());

        assertThat(customerService.findCustomerByLastname(from.getLastName()).getCourses())
                .size().isEqualTo(0);
        assertThat(customerService.findCustomerByLastname(to.getLastName()).getCourses())
                .size().isEqualTo(2);
    }

    @Test
    void cancelMembershipSuccess() throws CustomerNotFoundException, CourseNotFoundException, MembershipMailNotSent {
        // set up customer and course here
        // ...

        // configure MailGateway-mock
        when(mailGateway.sendMail(anyString(), anyString(), anyString())).thenReturn(true);

        courseService.cancelMembership(new CustomerNumber(1L), new CourseNumber(1L));
    }

    @Test
    void cancelMembershipFailBecauseOfUnableToSendMail() throws CustomerNotFoundException {
        // set up customer and course here
        // ...
        courseRepository.save(testCourse);
        customerRepository.save(testCust);
        courseService.enrollInCourse(testCust.getLastName(), testCourse);

        // configure MailGateway-mock
        when(mailGateway.sendMail(anyString(), anyString(), anyString())).thenReturn(false);

        assertThatExceptionOfType(MembershipMailNotSent.class)
                .isThrownBy(() -> courseService.cancelMembership(new CustomerNumber(testCust.getId()), new CourseNumber(testCourse.getId())))
                .withMessageContaining("Could not send membership mail to");
    }
    
    @Test
    void cancelMembershipSuccessBDDStyle() throws CustomerNotFoundException, CourseNotFoundException, MembershipMailNotSent {
        // set up customer and course here
        // ...

        // configure MailGateway-mock with BDD-style
        given(mailGateway.sendMail(anyString(), anyString(), anyString())).willReturn(true);

        courseService.cancelMembership(new CustomerNumber(1L), new CourseNumber(1L));
    }

    @Test
    void cancelMembershipSuccessCheckMembNum() throws CustomerNotFoundException, CourseNotFoundException, MembershipMailNotSent {
        courseRepository.save(testCourse);
        customerRepository.save(testCust);
        courseService.enrollInCourse(testCust.getLastName(), testCourse);

        assertEquals(1, testCourse.getAnzahlTeilnehmer());
        when(mailGateway.sendMail(anyString(), anyString(), anyString())).thenReturn(true);
        courseService.cancelMembership(new CustomerNumber(testCust.getId()), new CourseNumber(testCourse.getId()));

        Course updatedCourse = courseRepository
                .findById(testCourse.getId())
                .orElseThrow();
        assertEquals(0, updatedCourse.getAnzahlTeilnehmer());
    }
}