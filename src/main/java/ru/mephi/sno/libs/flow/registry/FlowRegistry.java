package ru.mephi.sno.libs.flow.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mephi.sno.libs.flow.belly.FlowBuilder;
import ru.mephi.sno.libs.flow.util.ConcurrentDualKeyMap;

import java.util.List;

public class FlowRegistry {

	private static final Logger log = LoggerFactory.getLogger(FlowRegistry.class);
	private final ConcurrentDualKeyMap<String, Class<?>, FlowBuilder> flowBuilders;

	private FlowRegistry() {
		flowBuilders = new ConcurrentDualKeyMap<>();
		log.info("FlowRegistry initialized.");
	}

	private static class FlowRegistrySingletonHolder {
		public static final FlowRegistry HOLDER_INSTANCE = new FlowRegistry();
	}

	public static FlowRegistry getInstance() {
		return FlowRegistrySingletonHolder.HOLDER_INSTANCE;
	}

	public void register(String name, Class<?> clazz, FlowBuilder flowBuilder) {
		if (name == null || name.isEmpty())
			throw new IllegalArgumentException("Flow name cannot be null or empty");
		if (flowBuilder == null)
			throw new IllegalArgumentException("FlowBuilder cannot be null");
		if (clazz == null)
			throw new IllegalArgumentException("Clazz cannot be null");
		if (flowBuilders.containsKey(name, clazz))
			throw new IllegalStateException("Flow already registered by name=" + name + ", class=" + clazz.getName());
		flowBuilders.put(name, clazz, flowBuilder);
		log.info("Registered flow: {}", name);
	}

	public FlowBuilder getFlow(String name) {
		return flowBuilders.getByKey1(name);
	}

	public FlowBuilder getFlow(Class<?> clazz) {
		return flowBuilders.getByKey2(clazz);
	}

	public List<String> getFlowNames() {
		return List.copyOf(flowBuilders.key1Set());
	}
}
