package org.iphukan.ubforms.data;


import org.iphukan.ubforms.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Validator {
	
	private UrSqlHelper sqlHelper;
	private Entity entity;
	
	public Validator(UrSqlHelper sqlHelper, Entity entity) {
		this.sqlHelper = sqlHelper;
		this.entity = entity;
	}
	
	public String validate(List<Attribute> attributes, Map<String, String> values) {

		List<Attribute> keys = new ArrayList<Attribute>();
		for (Attribute attribute: attributes) {

			String value = values.get(attribute.getAttributeName());
			if (value == null || value.trim().length() == 0) value = null;

			if (attribute.isRequired() && value == null) {
                return String.format("%s is required", attribute.getAttributeName());
            }

			String regex = attribute.getValidationRegex();
			if (regex == null || regex.trim().length() == 0) regex = null;

			if (value != null && regex != null) {
				boolean matches = Pattern.matches(regex, value);
				if (!matches) {
					return String.format("%s value %s is invalid. Example: %s",attribute.getAttributeName(),value,attribute.getValidationExample());
				}
			}

			if (attribute.isPrimaryKeyPart()) {
				keys.add(attribute);
			}

		}

		// Check unique constraint
		if (keys.size() > 0) {
			Map<String, String> checkValues = new HashMap<String, String>();
			for (Attribute key: keys) {
				checkValues.put(key.getAttributeName(), values.get(key.getAttributeName()));
			}
			List<Map<String, String>> results;
			DataDao dataDao = new DataDao(sqlHelper.getWritableDatabase());
			try {
				results = dataDao.search(entity, checkValues);
			} finally {
				sqlHelper.close();
			}
			if (results.size() > 0) {
				String message = "";
				for (Attribute key: keys) {
					message += String.format("A record already exists for, key: %s=%s",key.getAttributeName(),values.get(key.getAttributeName()));
				}
				return message;
			}
		}

		return null;
	}

    public String validate_dontcare_unique(List<Attribute> attributes, Map<String, String> values) {

        List<Attribute> keys = new ArrayList<Attribute>();
        for (Attribute attribute: attributes) {

            String value = values.get(attribute.getAttributeName());
            if (value == null || value.trim().length() == 0) value = null;

            if (attribute.isRequired() && value == null){
                return String.format("%s is required",attribute.getAttributeName());
            }

            String regex = attribute.getValidationRegex();
            if (regex == null || regex.trim().length() == 0) regex = null;

            if (value != null && regex != null) {
                boolean matches = Pattern.matches(regex, value);
                if (!matches) {
                    return String.format("%s value %s is invalid. Example: %s",attribute.getAttributeName(),value,attribute.getValidationExample());
                }
            }

            if (attribute.isPrimaryKeyPart()) {
                keys.add(attribute);
            }

        }

        return null;
    }

	
}
