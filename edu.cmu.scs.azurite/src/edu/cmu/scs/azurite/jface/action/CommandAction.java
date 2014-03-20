package edu.cmu.scs.azurite.jface.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IParameter;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.Parameterization;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;

public class CommandAction extends Action {
	
	private Map<String, String> parameters;
	
	public CommandAction(String text, String commandId) {
		this(text, commandId, null);
	}
	
	public CommandAction(String text, String commandId, Map<String, String> parameters) {
		setText(text);
		setActionDefinitionId(commandId);
		
		this.parameters = parameters == null ? null
				: new HashMap<String, String>(parameters);
	}
	
	@Override
	public void run() {
		IHandlerService handlerService = (IHandlerService) PlatformUI
				.getWorkbench().getService(IHandlerService.class);
		
		if (parameters != null) {
			ICommandService commandService = (ICommandService) PlatformUI
					.getWorkbench().getService(ICommandService.class);
			
			Command command = commandService.getCommand(getActionDefinitionId());
			
			// Get the Parameterization array.
			List<Parameterization> paramList = new ArrayList<Parameterization>();
			try {
				for (IParameter param : command.getParameters()) {
					if (parameters.containsKey(param.getId())) {
						paramList.add(new Parameterization(param, parameters.get(param.getId())));
					} else {
						paramList.add(new Parameterization(param, null));
					}
				}
			} catch (NotDefinedException e) {
			}
			
			// Create a ParameterizedCommand object.
			ParameterizedCommand paramCmd = new ParameterizedCommand(command, paramList.toArray(new Parameterization[0]));
			try {
				handlerService.executeCommand(paramCmd, null);
			} catch (ExecutionException | NotDefinedException
					| NotEnabledException | NotHandledException e) {
				e.printStackTrace();
			}
		} else {
			try {
				handlerService.executeCommand(getActionDefinitionId(), null);
			} catch (ExecutionException | NotDefinedException | NotEnabledException
					| NotHandledException e) {
				e.printStackTrace();
			}
		}
	}
}
