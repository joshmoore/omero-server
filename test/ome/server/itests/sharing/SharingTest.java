/*
 *   $Id$
 *
 *   Copyright 2008 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */
package ome.server.itests.sharing;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import ome.api.IShare;
import ome.conditions.SecurityViolation;
import ome.model.containers.Dataset;
import ome.model.internal.Permissions;
import ome.model.meta.Experimenter;
import ome.parameters.Filter;
import ome.server.itests.AbstractManagedContextTest;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * 
 */
@Test(groups = { "sharing" })
public class SharingTest extends AbstractManagedContextTest {

    private static Filter justOne = new Filter().page(0, 1);

    protected IShare share;

    @BeforeMethod
    public void setup() {
        share = factory.getShareService();
    }

    @Test
    public void testMembershipFunctions() {

        Experimenter nonMember = loginNewUser();
        Experimenter secondMember = loginNewUser();
        Experimenter firstMember = loginNewUser();
        Experimenter owner = loginNewUser();

        String firstGuest = "example1@example.com";
        String secondGuest = "example2@example.com";

        Dataset d = new Dataset("Dataset for share");
        d.getDetails().setPermissions(Permissions.USER_PRIVATE);
        d = iUpdate.saveAndReturnObject(d);

        long id = share.createShare("description", null, Collections
                .singletonList(d), Arrays.asList(firstMember, secondMember),
                Arrays.asList(firstGuest, secondGuest), true);

        // Members

        assertEquals(1, share.getSharesOwnedBy(owner, true).size());
        assertEquals(0, share.getMemberSharesFor(owner, true).size());
        assertEquals(0, share.getSharesOwnedBy(firstMember, true).size());
        assertEquals(1, share.getMemberSharesFor(firstMember, true).size());
        assertEquals(0, share.getSharesOwnedBy(firstMember, true).size());
        assertEquals(1, share.getMemberSharesFor(firstMember, true).size());
        assertEquals(0, share.getSharesOwnedBy(nonMember, true).size());
        assertEquals(0, share.getMemberSharesFor(nonMember, true).size());
        assertEquals(2, share.getAllMembers(id).size());
        boolean foundFirst = false;
        boolean foundSecond = false;
        for (Experimenter e : share.getAllMembers(id)) {
            if (e.getId().equals(firstMember.getId())) {
                foundFirst = true;
            } else if (e.getId().equals(secondMember.getId())) {
                foundSecond = true;
            }
        }
        assertTrue(foundFirst);
        assertTrue(foundSecond);

        share.removeUser(id, secondMember);

        assertEquals(1, share.getSharesOwnedBy(owner, true).size());
        assertEquals(0, share.getMemberSharesFor(owner, true).size());
        assertEquals(0, share.getSharesOwnedBy(firstMember, true).size());
        assertEquals(1, share.getMemberSharesFor(firstMember, true).size());
        assertEquals(0, share.getSharesOwnedBy(secondMember, true).size());
        assertEquals(0, share.getMemberSharesFor(secondMember, true).size());
        assertEquals(0, share.getSharesOwnedBy(nonMember, true).size());
        assertEquals(0, share.getMemberSharesFor(nonMember, true).size());
        foundFirst = false;
        foundSecond = false;
        for (Experimenter e : share.getAllMembers(id)) {
            if (e.getId().equals(firstMember.getId())) {
                foundFirst = true;
            } else if (e.getId().equals(secondMember.getId())) {
                foundSecond = true;
            }
        }
        assertTrue(foundFirst);
        assertFalse(foundSecond);

        share.addUser(id, secondMember);

        assertEquals(0, share.getSharesOwnedBy(owner, true).size());
        assertEquals(1, share.getMemberSharesFor(owner, true).size());
        assertEquals(0, share.getSharesOwnedBy(firstMember, true).size());
        assertEquals(1, share.getMemberSharesFor(firstMember, true).size());
        assertEquals(0, share.getSharesOwnedBy(secondMember, true).size());
        assertEquals(1, share.getMemberSharesFor(secondMember, true).size());
        assertEquals(0, share.getSharesOwnedBy(nonMember, true).size());
        assertEquals(0, share.getMemberSharesFor(nonMember, true).size());
        assertTrue(share.getAllMembers(id).contains(firstMember.getId()));
        assertTrue(share.getAllMembers(id).contains(secondMember.getId()));
        for (Experimenter e : share.getAllMembers(id)) {
            if (e.getId().equals(firstMember.getId())) {
                foundFirst = true;
            } else if (e.getId().equals(secondMember.getId())) {
                foundSecond = true;
            }
        }
        assertTrue(foundFirst);
        assertTrue(foundSecond);

        // Guests

        assertEquals(2, share.getAllGuests(id));
        assertTrue(share.getAllGuests(id).contains(firstGuest));
        assertTrue(share.getAllGuests(id).contains(secondGuest));

        share.removeGuest(id, secondGuest);

        assertEquals(1, share.getAllGuests(id));
        assertTrue(share.getAllGuests(id).contains(firstGuest));
        assertFalse(share.getAllGuests(id).contains(secondGuest));

        share.addGuest(id, secondGuest);

        assertEquals(2, share.getAllGuests(id));
        assertTrue(share.getAllGuests(id).contains(firstGuest));
        assertTrue(share.getAllGuests(id).contains(secondGuest));

        // All users

        Set<String> names = share.getAllUsers(id);
        assertTrue(names.contains(firstGuest));
        assertTrue(names.contains(secondGuest));
        assertTrue(names.contains(firstMember.getOmeName()));
        assertTrue(names.contains(secondMember.getOmeName()));

    }

    @Test
    public void testOnlyMembersAndGuestsCanActivateShare() {

        Experimenter nonMember = loginNewUser();
        Experimenter member = loginNewUser();
        Experimenter owner = loginNewUser();

        Dataset d = new Dataset("Dataset for share");
        d.getDetails().setPermissions(Permissions.USER_PRIVATE);
        d = iUpdate.saveAndReturnObject(d);

        long id = share.createShare("description", null, Collections
                .singletonList(d), Collections.singletonList(member), null,
                true);

        loginUser(owner.getOmeName());
        share.activate(id);
        iQuery.get(Dataset.class, d.getId());

        loginUser(member.getOmeName());
        share.activate(id);
        iQuery.get(Dataset.class, d.getId());

        loginUser(nonMember.getOmeName());
        try {
            share.activate(id);
            fail("Should not be allowed");
        } catch (SecurityViolation e) {
            // ok
        }
        try {
            iQuery.get(Dataset.class, d.getId());
            fail("Should not be allowed");
        } catch (SecurityViolation e) {
            // ok
        }
    }

    @Test
    public void testShareCreationAndViewing() {

        // New user who should be able to see the share.
        Experimenter e = loginNewUser();

        loginRoot();
        Dataset d = new Dataset("Dataset for share");
        d.getDetails().setPermissions(Permissions.USER_PRIVATE);
        d = iUpdate.saveAndReturnObject(d);

        long id = share.createShare("description", null, Collections
                .singletonList(d), null, null, true);

        loginUser(e.getOmeName());
        share.activate(id);
        iQuery.get(Dataset.class, d.getId());

    }

    @Test
    public void testWhoCanDoWhat() {
        fail("NYI");
    }
}