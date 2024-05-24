package ru.mephi.sno.libs.flow.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mephi.sno.libs.flow.belly.FlowBuilder;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class FlowRegistry {

	private static final Logger log = LoggerFactory.getLogger(FlowRegistry.class);
	private final ConcurrentHashMap<String, FlowBuilder> flowBuilders;

	private FlowRegistry() {
		flowBuilders = new ConcurrentHashMap<>();
		log.info("FlowRegistry initialized");
	}

	private static class FlowRegistrySingletonHolder {
		public static final FlowRegistry HOLDER_INSTANCE = new FlowRegistry();
	}

	public static FlowRegistry getInstance() {
		return FlowRegistrySingletonHolder.HOLDER_INSTANCE;
	}

	public void register(String name, FlowBuilder flowBuilder) {
		if (name == null || name.isEmpty())
			throw new IllegalArgumentException("Flow name cannot be null or empty");
		if (flowBuilder == null)
			throw new IllegalArgumentException("FlowBuilder cannot be null");
		if (flowBuilders.containsKey(name))
			throw new IllegalStateException("Flow already registered: " + name);
		flowBuilders.put(name, flowBuilder);
		log.info("Registered flow: {}", name);
	}

	public FlowBuilder getFlow(String name) {
		return flowBuilders.get(name);
	}

	public List<String> getFlowNames() {
		return List.copyOf(flowBuilders.keySet());
	}

}
