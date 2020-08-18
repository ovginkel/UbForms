package org.iphukan.ubforms;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.iphukan.ubforms.data.Attribute;
import org.iphukan.ubforms.data.DataDao;
import org.iphukan.ubforms.data.Entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SearchResultsActivity extends BaseActivity {


	private TableLayout resultsViewLayoutContents;

	private String mEntityName;
	private String mAttributeName;
	private String mSelectMode = null;
	private Entity mEntity;
	private Entity mSelectedEntity;
	private Map<String, String> mSearchValues;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle bundle = getIntent().getExtras();

		mEntityName = bundle.getString(EditDataActivity.ENTITY_NAME);
		mAttributeName = bundle.getString(EditDataActivity.ATTRIBUTE_NAME);
		mSelectMode = bundle.getString(SELECT_MODE);

		LinearLayout rootView = new LinearLayout(this);
		rootView.setMinimumWidth(COL_MIN_WIDTH);
		rootView.setOrientation(LinearLayout.HORIZONTAL);

		populateData(bundle);

		setupResultsView(rootView);

		doSearch();

		setContentView(rootView);

	}

	private void populateData(Bundle bundle) {

		mEntity = new Entity();
		mEntity.setName(mEntityName);
		mEntity.setAttributes(getAttributes(mEntity));

		mSearchValues = new HashMap<String, String>();

		for (Attribute attribute: mEntity.getAttributes()) {

			String searchValue = null;

			if (SearchDataActivity.isSearchable(attribute)) {
				searchValue = bundle.getString(attribute.getAttributeName());
				if (searchValue == null || searchValue.trim().length() == 0) searchValue = null;
				if (searchValue != null) mSearchValues.put(attribute.getAttributeName(), searchValue);
			}

		}
	}


	private void setupResultsView(ViewGroup parent) {

		LinearLayout resultsViewLayout = new LinearLayout(this);
		resultsViewLayout.setOrientation(LinearLayout.VERTICAL);
		resultsViewLayout.setMinimumWidth(COL_MIN_WIDTH);
		parent.addView(resultsViewLayout);


		resultsViewLayoutContents = new TableLayout(this);
		resultsViewLayout.addView(resultsViewLayoutContents);

	}

	private void doSearch() {

		resultsViewLayoutContents.removeAllViews();

		List<Map<String, String>> results;
		DataDao dataDao = new DataDao(sqlHelper.getWritableDatabase());
		try {
			results = dataDao.search(mEntity, mSearchValues);
		} finally {
			sqlHelper.close();
		}
		populateResultsView(mEntity, results);

	}

	private static final class RefByData {
		public String id;
		public String title;
	}

	private List<SearchResultsActivity.RefByData> getRefByData(String refEntityName, String refAttributeName, String entityid) {
		List<SearchResultsActivity.RefByData> result = new ArrayList<SearchResultsActivity.RefByData>();
		try {
			Entity refEntity = new Entity();
			refEntity.setName(refEntityName);
			refEntity.setAttributes(this.getAttributes(refEntity));
			Map<String, String> refValues = new HashMap<String, String>();
			refValues.put(refAttributeName, entityid);

			List<Map<String, String>> results;
			DataDao dataDao = new DataDao(sqlHelper.getWritableDatabase());
			try {
				results = dataDao.searchExact(refEntity, refValues);
			} finally {
				sqlHelper.close();
			}
			for (Map<String, String> row: results) {
				SearchResultsActivity.RefByData rbd = new SearchResultsActivity.RefByData();
				if((!(row.get(refAttributeName)==null))?row.get(refAttributeName).equals(entityid):false){
                    rbd.id = row.get("_id");
                    rbd.title = this.getTitle(refEntityName, Long.valueOf(rbd.id), refAttributeName);
                    result.add(rbd);
                }
				else if(row.get("_id").equals(entityid)) {
					rbd.id = row.get("_id");
					rbd.title = this.getTitle(refEntityName, Long.valueOf(rbd.id), refAttributeName);
					result.add(rbd);
				}
			}
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}
		return result;
	}

	private static boolean isListable(Attribute attribute) {
		return attribute.isListable() && 
				(
						attribute.getDataType().equals(Attribute.STRING_TYPE) 
						|| attribute.getDataType().equals(Attribute.DATE_TYPE)
						|| attribute.getDataType().equals(Attribute.TIME_TYPE)
						|| attribute.getDataType().equals(Attribute.CHOICES_TYPE)
                        || attribute.getDataType().equals(Attribute.REF_BY_TYPE)
						|| attribute.getDataType().equals(Attribute.REF_TYPE)
				);
	}

	private String getSelectText() {
		return (mSelectMode == null) ? getString(R.string.edit) : getString(R.string.select);
	}

	private Attribute mSortOn;
	private boolean mSortDesc = false;

	private void populateResultsView(Entity entity, List<Map<String, String>> results) {

		List<Attribute> attributes = getAttributes(entity);
		List<Entity> entities = new ArrayList<Entity>();
		List<Entity> bentities = new ArrayList<Entity>();

		for (Map<String, String> result: results) {
			Map<String, String> result_ref = new HashMap<String, String>();
			for (Attribute attribute: attributes) {
				if (isListable(attribute)) {
					if (!result.containsKey(attribute.getAttributeName())) {
						result_ref.put(attribute.getAttributeName(), "");
						result.put(attribute.getAttributeName(), "");
					}
					else {
						if(attribute.getDataType().equals(Attribute.REF_TYPE) || attribute.getDataType().equals(Attribute.REF_BY_TYPE)) { //attribute.getDataType().equals(Attribute.REF_BY_TYPE) ||

							////
							String ref = attribute.getRefEntityName();

							if (!(ref == null || ref.trim().length() == 0)) // || ref.indexOf(" ") == -1))
							{
								String[] sa = ref.split(" ");
								String refEntityName = sa[0];
								String refAttributeName = "";
								String sid_str = "";
								if(attribute.getDataType().equals(Attribute.REF_BY_TYPE))
								{
									Entity refEntity = new Entity();
									refEntity.setName(refEntityName);
									List<Attribute> refAttributes = this.getAttributes(refEntity);
									for(Attribute ref_attribute: refAttributes) {
										if (ref_attribute.getDataType().equals(Attribute.REF_TYPE)) {
											String ref2 = ref_attribute.getRefEntityName();
											String[] sa2 = ref2.split(" ");
											if(!entity.getName().equals(sa2[0]))
											{
												refAttributeName = sa[1];
												//refEntityName_new = sa2[0];
												sid_str = result.get("_id"); //result.get("_id"); //result.get(ref_attribute.getAttributeName());
											}
										}
									}
								}
								else  {
									Entity refEntity = new Entity();
									refEntity.setName(refEntityName);
									List<Attribute> refAttributes = this.getAttributes(refEntity);
									for(Attribute ref_attribute: refAttributes)
									{
										if(ref_attribute.getDataType().equals(Attribute.REF_BY_TYPE))
										{

											String ref2 = ref_attribute.getRefEntityName();
											String[] sa2 = ref2.split(" ");
											if(entity.getName().equals(sa2[0]))
											{
												refAttributeName = sa2[1];
												sid_str = result.get(attribute.getAttributeName());
											}
										}
									}
									//refAttributeName = "meeting"; //entity.getName(); //.getAttributeName();
								}
								List<RefByData> list = new ArrayList<RefByData>();
								//String sid_str = result.get(attribute.getAttributeName());
								//int sid = Integer.parseInt(sid_str);
								//if (sid_str != null) {
									if(attribute.getDataType().equals(Attribute.REF_TYPE))
									{
										list = getRefByData(refEntityName, refAttributeName, sid_str);
									}
									else
									{
										list = getRefByData(refEntityName, refAttributeName, sid_str);
									}
								//}
								String txt = "";
								for (SearchResultsActivity.RefByData rbd : list) {
									txt += "(" + rbd.id + ") " + rbd.title + "\r\n";
								}
								if (txt.length() > 2) {
									txt = txt.substring(0, txt.length() - 2);
								}

								/////
								result_ref.put(attribute.getAttributeName(), txt);
							}
							else {
								result_ref.put(attribute.getAttributeName(), result.get(attribute.getAttributeName()));
							}
						}
						else
						{
							result_ref.put(attribute.getAttributeName(), result.get(attribute.getAttributeName()));
						}
					}
				} else {
					result.remove(attribute.getAttributeName());
					//result_ref.remove(attribute.getAttributeName());
				}
			}

			//Must copy id also...
			result_ref.put("_id", result.get("_id"));

			Entity aentity = new Entity();
			aentity.setName(entity.getName());
			aentity.setAttributes(attributes);
			aentity.setValues(result_ref);
			entities.add(aentity);


			Entity bentity = new Entity();
			bentity.setName(entity.getName());
			bentity.setAttributes(attributes);
			bentity.setValues(result);
			bentities.add(bentity);

		}

		if (mSortOn != null) {
			final boolean isAlphaSort = isAlphaSort(mSortOn);
			Collections.sort(entities, new Comparator<Entity>() {
				public int compare(Entity e1, Entity e2) {
					String v1 = e1.getValues().get(mSortOn.getAttributeName());
					if (v1 == null) v1 = "";
					String v2 = e2.getValues().get(mSortOn.getAttributeName());
					if (v2 == null) v2 = "";
					int retval = 0;
					if (isAlphaSort) {
						retval = v1.compareTo(v2);
					} else {
						try {
							retval = new BigDecimal(v1).compareTo(new BigDecimal(v2));
						} catch (Exception e) {
							retval = v1.compareTo(v2);
						}
					}
					if (mSortDesc) retval = -retval;
					return retval;
				}
			});
		}


		// Views
		resultsViewLayoutContents.removeAllViews();
		TableLayout tableLayout = new TableLayout(this);

		// Refresh button
		TableRow tr = new TableRow(this);
		tableLayout.addView(tr);
		Button bv = new Button(this);
		bv.setText(R.string.refresh);
		bv.setTextSize(TEXT_SIZE_LARGE);
		bv.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				doSearch();
			}
		});
		tr.addView(bv);

		// Sort buttons
		for (Attribute attribute:attributes) {
			if (isListable(attribute)) {
				final Attribute sort = attribute;
				bv = new Button(this);
				bv.setText(attribute.getAttributeDesc());
				bv.setTextSize(TEXT_SIZE_LARGE);
				bv.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						if (mSortOn == null) {
							mSortDesc = false;
						} else {
							if (mSortOn.getAttributeName().equals(sort.getAttributeName())) mSortDesc = !mSortDesc;
						}
						mSortOn = sort;
						doSearch();
					}
				});
				tr.addView(bv);
			}
		}

		// Data
		for (Entity aentity: entities) {
			tr = new TableRow(this);
			bv = new Button(this);
			bv.setText(getString(R.string.entity_arrow,getSelectText()));
			bv.setTextSize(TEXT_SIZE_LARGE);
			String aid = aentity.getValues().get("_id");
			Entity mbentity=null;
			for(Entity bentity:bentities){
				if(aid.equals(bentity.getValues().get("_id"))) {
					mbentity=bentity;
					break;
				}
			}
			final Entity fentity = mbentity;

			View.OnClickListener ocl;
			if (this.mSelectMode == null) {
				ocl = new View.OnClickListener() {
					public void onClick(View v) {
						startEditExisting(fentity);
					}
				};
			} else {
				ocl = new View.OnClickListener() {
					public void onClick(View v) {
						mSelectedEntity = fentity;
						finish();
					}
				};
			}
			bv.setOnClickListener(ocl);
			tr.addView(bv);

			for (Attribute attribute:aentity.getAttributes()) {
				if (isListable(attribute)) {
					String value = aentity.getValues().get(attribute.getAttributeName());
					TextView tv = new TextView(this);
					tv.setText(value);
					tv.setTextSize(TEXT_SIZE_LARGE);
					tv.setPadding(2, 2, 2, 2);
					tr.addView(tv);
				}
			}
			tableLayout.addView(tr);
		}

		resultsViewLayoutContents.addView(tableLayout);

	}

	private static boolean isAlphaSort(Attribute attribute) {
		/*String v = attribute.getValidationExample();
		if (v == null || v.trim().length() == 0) return true;
		for (int i = 0; i < v.length(); i++) {
			char c = v.charAt(i);
			if (Character.isLetter(c)) return true;
		}
		String v2 = attribute.getValidationRegex();
		if(v2.contains("\\d") && (!v2.contains("\\w")))
		{
			for (int i = 0; i < 26; i++) {
				char c = v2.charAt(i);
				if (Character.isLetter(c))
				{
					if("d".contains(c+"") && i>0)
					{
						char c2 = v2.charAt(i-1);
						if("\\".contains(c+""))
						{
							//still ok
						}
						else{
							return true;
						}
					}
					else
					{
						return true;
					}
				}
			}
		}*/
		return false;
	}
	
	
	private boolean hadFocus = false;
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		if (hasFocus && hadFocus) {
			doSearch();
		}
		hadFocus = true;
	}

	@Override
	public void finish() {
		Intent intent = new Intent();
		Bundle bundle = getEntitySelectedBundle(mSelectedEntity);
		bundle.putString(EditDataActivity.ENTITY_NAME, mEntityName); // this entity type
		bundle.putString(EditDataActivity.ATTRIBUTE_NAME, mAttributeName); //  fk attribute on caller, not this entity
		intent.putExtras(bundle);
		setResult(RESULT_OK, intent);
		super.finish();
	} 

}
