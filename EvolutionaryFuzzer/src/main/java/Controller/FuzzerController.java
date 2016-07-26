package Controller;

import Analyzer.IsGoodRule;
import Executor.ExecutorThreadPool;
import Mutator.Mutator;
import Controller.Controller;
import Config.EvolutionaryFuzzerConfig;
import Graphs.BranchTrace;
import Graphs.CountEdge;
import Graphs.Edge;
import Graphs.ProbeVertex;
import de.rub.nds.tlsattacker.tls.config.ConfigHandler;
import de.rub.nds.tlsattacker.tls.config.ConfigHandlerFactory;
import de.rub.nds.tlsattacker.tls.workflow.TlsContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import Result.ResultContainer;
import Server.ServerManager;
import Mutator.SimpleMutator;
import Server.TLSServer;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Set;
import org.jgrapht.DirectedGraph;

/**
 * Currently only Implementation of the Controller Interface which controls the
 * complete executions
 * 
 * @author Robert Merget - robert.merget@rub.de
 */
public class FuzzerController extends Controller {

    private static final Logger LOG = Logger.getLogger(FuzzerController.class.getName());

    // Chosen Mutator
    private final Mutator mutator;
    // ThreadPool to start or stop
    private final ExecutorThreadPool pool;

    /**
     * Basic Constructor, initializes the Server List, generates the necessary
     * Config Files and Contexts and also commints to a mutation Engine
     * 
     * @param config
     *            Configuration used by the Controller
     */
    public FuzzerController(EvolutionaryFuzzerConfig config) {
	super(config);
	ServerManager serverManager = ServerManager.getInstance();
	serverManager.init(config);

	ConfigHandler configHandler = ConfigHandlerFactory.createConfigHandler("client");
	TlsContext tmpTlsContext = configHandler.initializeTlsContext(new EvolutionaryFuzzerConfig());
	mutator = new SimpleMutator(tmpTlsContext, config);
	int threads = config.getThreads();
	if (threads == -1) {
	    threads = serverManager.getNumberOfServers();
	}
	pool = new ExecutorThreadPool(threads, mutator, config);
	Thread t = new Thread(pool);
	t.setName("Executor Thread Pool");
	t.start();
    }

    /**
     * Starts the Fuzzer
     */
    @Override
    public void startFuzzer() {
	this.isRunning = false;
	pool.setStopped(false);
    }

    /**
     * Stops the Fuzzer
     */
    @Override
    public void stopFuzzer() {
	this.isRunning = false;
	pool.setStopped(true);
    }

    @Override
    public void startConsoleInput() {
	BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	while (true) {
	    String s = null;
	    try {
		System.out.print(">");
		s = br.readLine();
	    } catch (IOException ex) {
		Logger.getLogger(FuzzerController.class.getName()).log(Level.SEVERE, null, ex);
	    }
	    String[] split = s.split(" ");
	    if (split.length > 0) {
		switch (split[0]) {
		    case "start":
			startFuzzer();

			break;
		    case "stop":
			LOG.log(Level.INFO, "Stopping Fuzzer!");
			stopFuzzer();
			do {
			    try {
				Thread.sleep(50);
			    } catch (InterruptedException ex) {
				Logger.getLogger(FuzzerController.class.getName()).log(Level.SEVERE, null, ex);
			    }
			} while (pool.hasRunningThreads());
			LOG.log(Level.INFO, "Fuzzer stopped!");
			break;
		    case "status":
			ResultContainer con = ResultContainer.getInstance();
			System.out.println(con.getAnalyzer().getReport());
			break;
		    case "server":
			List<TLSServer> serverList = ServerManager.getInstance().getAllServers();
			for (TLSServer server : serverList) {
			    System.out.println(server);
			}
			break;
		    case "edges":
			String file = "edges.dump";
			if (split.length == 2) {
			    file = split[1];
			}
			LOG.log(Level.INFO, "Dumping Edge Information to " + file);
			stopFuzzer();
			do {
			    try {
				Thread.sleep(50);
			    } catch (InterruptedException ex) {
				Logger.getLogger(FuzzerController.class.getName()).log(Level.SEVERE, null, ex);
			    }
			} while (pool.hasRunningThreads());

			BranchTrace trace = ((IsGoodRule) ((ResultContainer.getInstance().getAnalyzer()
				.getRule(IsGoodRule.class)))).getBranchTrace();

			PrintWriter writer;
			try {
			    writer = new PrintWriter(file, "UTF-8");
			    Map<Edge, Edge> set = trace.getEdgeMap();
			    for (Edge edge : set.values()) {
				writer.println(edge.getA() + " " + edge.getB());
			    }
			    writer.close();
			} catch (FileNotFoundException ex) {
			    Logger.getLogger(FuzzerController.class.getName()).log(Level.SEVERE, null, ex);
			} catch (UnsupportedEncodingException ex) {
			    Logger.getLogger(FuzzerController.class.getName()).log(Level.SEVERE, null, ex);
			}
			LOG.log(Level.INFO, "Dump finished");
			startFuzzer();
			break;
		    case "vertices":
			file = "vertices.dump";
			if (split.length == 2) {
			    file = split[1];
			}
			LOG.log(Level.INFO, "Dumping Vertex Information to " + file);
			stopFuzzer();
			do {
			    try {
				Thread.sleep(50);
			    } catch (InterruptedException ex) {
				Logger.getLogger(FuzzerController.class.getName()).log(Level.SEVERE, null, ex);
			    }
			} while (pool.hasRunningThreads());

			trace = ((IsGoodRule) ((ResultContainer.getInstance().getAnalyzer().getRule(IsGoodRule.class))))
				.getBranchTrace();
			writer = null;
			try {
			    writer = new PrintWriter(file, "UTF-8");
			    Set<Long> set = trace.getVerticesSet();
			    for (Long vertex : set) {
				writer.println(vertex);
			    }
			    writer.close();
			} catch (FileNotFoundException ex) {
			    Logger.getLogger(FuzzerController.class.getName()).log(Level.SEVERE, null, ex);
			} catch (UnsupportedEncodingException ex) {
			    Logger.getLogger(FuzzerController.class.getName()).log(Level.SEVERE, null, ex);
			}
			LOG.log(Level.INFO, "Dump finished");
			startFuzzer();
			break;
		    default:
			System.out
				.println("Commands: start, stop, status, server, edges <file>, vertices <file>, loadGraph <file>, saveGraph <file>");
			break;
		}
	    }
	}
    }

}
