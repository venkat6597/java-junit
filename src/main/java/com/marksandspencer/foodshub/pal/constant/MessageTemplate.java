package com.marksandspencer.foodshub.pal.constant;

/**
 * The enum for Message Template
 */
public enum MessageTemplate {

	/**
	 * Template for Add Project.
	 */
	ADD_PROJECT("AddProject", "NEW_PAL_PROJECT","'%s' Project created in PAL"),

	/**
	 * Template for Project Status Change.
	 */
	UPDATE_PROJECT_STATUS("UpdateProjectStatus", "UPDATE_PROJECT_STATUS", "'%s' Project Status changed to '%s'"),

	/**
	 * Template for Project Status Change.
	 */
	UPDATE_PRODUCT_STATUS("UpdateProductStatus", "UPDATE_PRODUCT_STATUS", "'%s' Product Status changed to '%s'"),

	/**
	 * Template for Add Product.
	 */
	ADD_PRODUCT("AddProduct", "NEW_PAL_PRODUCT", "'%s' Product added to '%s' Project");

	private String templateId;
	private String eventName;
	private String messageSubject;

	MessageTemplate(final String templateId,final String eventName,final String messageSubject) {
		this.templateId = templateId;
		this.eventName = eventName;
		this.messageSubject = messageSubject;
	}

	/**
	 * Gets template Name.
	 *
	 * @return the template name
	 */
	public String getTemplateId() {
		return templateId;
	}

	/**
	 * Gets event Name.
	 *
	 * @return the event name
	 */
	public String getEventName() {
		return eventName;
	}

	/**
	 * Gets message subject.
	 *
	 * @return the message subject
	 */
	public String getMessageSubject() {
		return messageSubject;
	}
}
