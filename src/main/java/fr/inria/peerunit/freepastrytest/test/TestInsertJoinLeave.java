package fr.inria.peerunit.freepastrytest.test;

import fr.inria.peerunit.freepastrytest.PastryPeer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import rice.environment.Environment;
import rice.p2p.commonapi.Id;
import rice.p2p.past.PastContent;
import rice.tutorial.past.MyPastContent;
import fr.inria.peerunit.freepastrytest.util.FreeLocalPort;
import fr.inria.peerunit.parser.AfterClass;
import fr.inria.peerunit.parser.TestStep;
import fr.inria.peerunit.tester.Assert;
import fr.inria.peerunit.util.TesterUtil;
import static fr.inria.peerunit.tester.Assert.*;

/**
 * Test Insert/Retrieve in a Shrinking System
 * @author almeida
 *
 */
public class TestInsertJoinLeave extends AbstractFreePastryTest {

    private static Logger log = Logger.getLogger(TestInsertJoinLeave.class.getName());
    private Map<Integer, Object> objList = new HashMap<Integer, Object>();
    private List<String> expecteds = new ArrayList<String>();
    private List<PastContent> keySet;

    @TestStep(range = "*", timeout = 10000, order = 1)
    public void startingNetwork() throws UnknownHostException, InterruptedException, RemoteException, IOException {

        if (this.getPeerName() == 0) {
            log.info("I am " + this.getPeerName());
            //	Loads pastry settings
            Environment env = new Environment();

            // the port to use locally
            FreeLocalPort port = new FreeLocalPort();
            int bindport = port.getPort();
            log.info("LocalPort:" + bindport);

            // build the bootaddress from the command line args
            InetAddress bootaddr = InetAddress.getByName(defaults.getBootstrap());
            Integer bootport = new Integer(defaults.getBootstrapPort());
            InetSocketAddress bootaddress;

            bootaddress = new InetSocketAddress(bootaddr, bootport.intValue());

            peer = new PastryPeer(bootaddress);

            if (!peer.bootsrap()) {
                inconclusive("I couldn't become a boostrapper, sorry");
            }

            this.put(-2, bootaddress);
            //log.info("Cached boot address: "+bootaddress.toString());
            //this.put(-1,bootaddress);
            log.info("Net created");

            while (!peer.isReady()) {
                Thread.sleep(sleep);
            }
        }
        Thread.sleep(sleep);

    }

    @TestStep(range = "0", timeout = 10000, order = 2)
    public void chosingPeer() throws RemoteException {
        Random rand = new Random();
        List<Integer> generated = new ArrayList<Integer>();
        int chosePeer;
        int netSize = (defaults.getExpectedTesters() * defaults.getChurnPercentage()) / 100;
        log.info("It will join " + netSize + " peers");
        boolean peerChose;
        while (netSize > 0) {
            peerChose = false;
            while (!peerChose) {
                chosePeer = rand.nextInt(defaults.getExpectedTesters());
                if (chosePeer != 0) {
                    Integer genInt = new Integer(chosePeer);
                    if (!generated.contains(genInt)) {
                        generated.add(genInt);
                        peerChose = true;
                        log.info("Chose peer " + genInt);
                    }
                }
            }
            netSize--;
        }
        for (Integer intObj : generated) {
            this.put(intObj.intValue() * 100, intObj);
            if (intObj.intValue() % 2 == 0) {
                log.info("leave " + intObj.intValue());
            } else {
                log.info("join " + intObj.intValue());
            }
        }
    }

    @TestStep(range = "*", timeout = 10000, order = 3)
    public void startingInitNet() throws InterruptedException, RemoteException, IOException {



        if (!chosenOne(this.getPeerName()).equalsIgnoreCase("join") && (this.getPeerName() != 0)) {
            log.info("Joining in first");
            //	Loads pastry settings
            Environment env = new Environment();

            // the port to use locally
            FreeLocalPort port = new FreeLocalPort();
            int bindport = port.getPort();
            log.info("LocalPort:" + bindport);

            Thread.sleep(this.getPeerName() * 1000);
            InetSocketAddress bootaddress = (InetSocketAddress) this.get(-2);
            log.info("Getting cached boot " + bootaddress.toString());
//            if (!peer.join(bindport, bootaddress, env, log)) {
//                inconclusive("Couldn't boostrap, sorry");
//            }
            log.info("Running on port " + peer.getPort());
            log.info("Time to bootstrap");

        }

    }

    @TestStep(range = "*", timeout = 10000, order = 4)
    public void testInsert() throws InterruptedException, RemoteException {

        Random random = new Random();
        Thread.sleep(sleep);
        if (this.getPeerName() == 0) {
            List<PastContent> resultSet = new ArrayList<PastContent>();

            // these variables are final so that the continuation can access them
            for (int i = 0; i < OBJECTS; i++) {
                final String s = "test" + random.nextInt();//peer.env.getRandomSource().nextInt();

                // build the past content
                //final PastContent myContent = new MyPastContent(peer.localFactory.buildId(s), s);
                //peer.insert(myContent);


                peer.put(s, s);
                //resultSet.add(myContent);

            }
            this.put(-1, resultSet);
        }

    }

    @TestStep(range = "*", timeout = 10000, order = 5)
    public void testRetrieve() throws RemoteException, InterruptedException {


        if (!chosenOne(this.getPeerName()).equalsIgnoreCase("join")) {
            Thread.sleep(sleep);

            // Lookup first time
            keySet = (List<PastContent>) this.get(-1);
            Id contentKey;
            for (PastContent key : keySet) {
                contentKey = key.getId();
                if (contentKey != null) {
                    log.info("[PastryTest] Lookup Expected " + contentKey.toString());
                    peer.lookup(contentKey);
                }
            }

            // Sleep

            Thread.sleep(sleep);


            log.info("[PastryTest] Retrieved so far " + peer.getResultSet().size());

            for (Object expected : peer.getResultSet()) {
                if (expected != null) {
                    log.info("Retrieve before depart " + expected.toString());
                    expecteds.add(expected.toString());
                }
            }
            if (this.getPeerName() == 0) {
                this.put(2, expecteds);
            }
        }


    }

    @TestStep(range = "*", timeout = 10000, order = 6)
    public void volatility() throws InterruptedException, RemoteException, IOException {


        Thread.sleep(sleep);

        if (chosenOne(this.getPeerName()).equalsIgnoreCase("join")) {
            log.info("Joining in second ");
            //	Loads pastry settings
            Environment env = new Environment();

            // the port to use locally
            FreeLocalPort port = new FreeLocalPort();
            int bindport = port.getPort();
            log.info("LocalPort:" + bindport);

            Thread.sleep(this.getPeerName() * 1000);
            InetSocketAddress bootaddress = (InetSocketAddress) this.get(-2);
            log.info("Getting cached boot " + bootaddress.toString());
//            if (!peer.join(bindport, bootaddress, env, log)) {
//                inconclusive("Couldn't boostrap, sorry");
//            }
            while (!peer.isAlive()) {
                log.info("I'm not ready yet ");
                Thread.sleep(sleep);
            }
            log.info("Running on port " + peer.getPort());
            log.info("Time to bootstrap");
        } else if (chosenOne(this.getPeerName()).equalsIgnoreCase("leave")) {
            log.info("Leaving early ");
            this.kill();
        }

    }

    @TestStep(range = "*", timeout = 10000, order = 7)
    public void testInitialRetrieve() throws RemoteException, InterruptedException {

        if (!chosenOne(this.getPeerName()).equalsIgnoreCase("leave")) {
            List<String> actuals = new ArrayList<String>();
            Thread.sleep(sleep);
            Id contentKey;
            for (PastContent key : keySet) {
                contentKey = key.getId();
                if (contentKey != null) {
                    log.info("[PastryTest] Lookup Expected " + contentKey.toString());
                    peer.lookup(contentKey);
                }
            }

            // Sleep

            Thread.sleep(sleep);


            log.info("[PastryTest] Retrieved so far " + peer.getResultSet().size());

            for (Object actual : peer.getResultSet()) {
                if (actual != null) {
                    log.info("Retrieve before depart " + actual.toString());
                    actuals.add(actual.toString());
                    this.put(this.getPeerName(), actuals);

                }
            }
        }


    }

    @TestStep(range = "*", timeout = 10000, order = 8)
    public void buildExpecteds() throws RemoteException {

        Set<Integer> newKeySet = this.getCollection().keySet();
        List<String> cached = new ArrayList<String>();
        Object obj;
        for (Integer key : newKeySet) {
            obj = this.get(key);
            if ((key.intValue() >= 0) && (key.intValue() < 64)) {
                cached = (List<String>) obj;
            }

            for (String cachedObj : cached) {
                if (!expecteds.contains(cachedObj)) {
                    expecteds.add(cachedObj);
                }
            }
        }
        log.info("I may find " + expecteds.size() + " objects");
        for (String exp : expecteds) {
            log.info("I may find " + exp);
        }


    }

    @TestStep(range = "*", timeout = 10000, order = 9)
    public void testRetrieveByOthers() throws InterruptedException, RemoteException {

        if (!chosenOne(this.getPeerName()).equalsIgnoreCase("leave")) {
            Thread.sleep(sleep);

            // Lookup first time

            Id contentKey;
            for (PastContent key : keySet) {
                contentKey = key.getId();
                if (contentKey != null) {
                    log.info("[PastryTest] Lookup Expected " + contentKey.toString());
                    peer.lookup(contentKey);
                }
            }

            Thread.sleep(sleep);

            log.info("[PastryTest] Retrieved so far " + peer.getResultSet().size());

            List<String> actuals = new ArrayList<String>();
            int timeToFind = 0;
            while (timeToFind < defaults.getLoopToFail()) {
                log.info("Retrieval " + timeToFind);
                for (Object actual : peer.getResultSet()) {
                    if (actual != null) {
                        log.info("[Local verdict] Actual " + actual.toString());

                        if (!actuals.contains(actual.toString())) {
                            actuals.add(actual.toString());
                        }
                    }
                }
                peer.pingNodes();
                Thread.sleep(sleep);
                timeToFind++;
            }

            //List<String> expecteds=(List<String>)this.get(2);
            log.info("[Local verdict] Waiting a Verdict. Found " + actuals.size() + " of " + expecteds.size());
            Assert.assertListEquals("[Local verdict] Arrays ", expecteds, actuals);
        }


    }

    @AfterClass(timeout = 100000, range = "*")
    public void end() {
        log.info("[PastryTest] Peer bye bye");
    }

    private String chosenOne(int name) throws RemoteException {

        if (objList.isEmpty()) {
            objList = this.getCollection();
        }
        Set<Integer> keySet = objList.keySet();
        Object nameChose;

        for (Integer key : keySet) {
            nameChose = objList.get(key);
            if (nameChose instanceof Integer) {
                Integer new_name = (Integer) nameChose;
                if (new_name.intValue() == name) {
                    if ((new_name.intValue() / 100) % 2 == 0) {
                        return "leave";
                    } else {
                        return "join";
                    }
                }
            }
        }

        return "remain";
    }
}
