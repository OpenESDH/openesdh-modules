package dk.openesdh.staff;

import com.tradeshift.test.remote.Remote;
import com.tradeshift.test.remote.RemoteTestRunner;
import dk.openesdh.repo.services.cases.CaseService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Created by rasmutor on 6/3/15.
 */
@RunWith(RemoteTestRunner.class)
@Remote(runnerClass = SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:alfresco/application-context.xml"})
public class StaffIT {

    @Autowired
    private CaseService caseService;

    @Before
    public void setUp() throws Exception {

    }
    @Test
    public void test1() throws Exception {
        Assert.assertNotNull(caseService);
    }
}
