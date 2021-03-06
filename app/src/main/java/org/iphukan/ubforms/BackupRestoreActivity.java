package org.iphukan.ubforms;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.iphukan.ubforms.common.DialogUtils;
import org.iphukan.ubforms.common.IDialogClickListener;
import org.iphukan.ubforms.common.IGrantPermissionCallback;
import org.iphukan.ubforms.data.Attribute;
import org.iphukan.ubforms.data.AttributeDao;
import org.iphukan.ubforms.data.BlobData;
import org.iphukan.ubforms.data.BlobDataDao;
import org.iphukan.ubforms.data.DataDao;
import org.iphukan.ubforms.data.Entity;
import org.iphukan.ubforms.data.EntityDao;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class BackupRestoreActivity extends BaseActivity {


	private String storagePath;
    public File blobDir = null;



	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		storagePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/ubforms";

		LinearLayout rootView = new LinearLayout(this);
		rootView.setOrientation(LinearLayout.VERTICAL);
		rootView.setMinimumWidth(COL_MIN_WIDTH);

		Button btnExport = new Button(this);
		btnExport.setText(getString(R.string.backup_data_csv));
        btnExport.setTextSize(TEXT_SIZE_LARGE);
		btnExport.setFocusableInTouchMode(true);
		btnExport.requestFocus();
		btnExport.setOnClickListener(new OnClickListener(){
			public void onClick(View view) {
				doExport();
			}
		});
		rootView.addView(btnExport);

		Button btnImport = new Button(this);
		btnImport.setText(getString(R.string.restore_data_csv));
        btnImport.setTextSize(TEXT_SIZE_LARGE);
		btnImport.setOnClickListener(new OnClickListener(){
			public void onClick(View view) {
				doImport();
			}
		});
		rootView.addView(btnImport);

		TextView tv1 = new TextView(this);
		tv1.setText(" ");
		rootView.addView(tv1);

		TextView tv2 = new TextView(this);
		tv2.setText(" ");
		rootView.addView(tv2);

		Button btnDeleteEntity = new Button(this);
		btnDeleteEntity.setText(R.string.delete_form);
        btnDeleteEntity.setTextSize(TEXT_SIZE_LARGE);
		final Context context = this;
		btnDeleteEntity.setOnClickListener(new OnClickListener(){
			public void onClick(View view) {
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setMessage(getString(R.string.are_sure_delete_form))
				.setCancelable(false)
				.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						doDeleteEntity();
					}
				})
				.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
				AlertDialog alert = builder.create();
				alert.show();
			}
		});
		rootView.addView(btnDeleteEntity);


		TextView tv3 = new TextView(this);
		tv3.setText(" ");
		rootView.addView(tv3);

		Button btnImportForms = new Button(this);
		btnImportForms.setText(R.string.import_forms);
        btnImportForms.setTextSize(TEXT_SIZE_LARGE);
		btnImportForms.setOnClickListener(new OnClickListener(){
			public void onClick(View view) {
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setMessage(getString(R.string.are_sure_replace_all_form_defs))
				.setCancelable(false)
				.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						AlertDialog.Builder builder = new AlertDialog.Builder(context);
						builder.setMessage(getString(R.string.confirm_all_form_defs))
						.setCancelable(false)
						.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								doImportForms();
							}
						})
						.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
						AlertDialog alert = builder.create();
						alert.show();

					}
				})
				.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
				AlertDialog alert = builder.create();
				alert.show();
			}
		});
		rootView.addView(btnImportForms);

		setContentView(rootView);

	}

	private void checkStoragePermission(Activity activity, final IGrantPermissionCallback callback) {
		Dexter.withActivity(activity)
				.withPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
				.withListener(new MultiplePermissionsListener() {
					@Override
					public void onPermissionsChecked(MultiplePermissionsReport report) {
						if (callback == null) return;
						if (report.isAnyPermissionPermanentlyDenied()) {
							callback.denied();
						} else {
							callback.granted();
						}
					}

					@Override
					public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
						token.continuePermissionRequest();
					}
				}).check();
	}

	private void doDeleteEntity() {

		final Context context = this;
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(R.string.delete_form);
		alert.setMessage(R.string.select_form_delete);
		final Spinner sp = new Spinner(this);
		final List<Entity> entities = getEntities();
		final String[] names = new String[entities.size()];
		for (int i = 0; i < names.length;i++) {
			names[i] = entities.get(i).getName();
		}
		ArrayAdapter<CharSequence> adapter = 
				new ArrayAdapter<CharSequence>(this,
						android.R.layout.simple_spinner_item,
						names);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sp.setAdapter(adapter);
		alert.setView(sp);
		alert.setPositiveButton(getString(R.string.delete_form), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				int pos = sp.getSelectedItemPosition();
				if (pos == Spinner.INVALID_POSITION) return;
				final String nameToDelete = names[pos];


				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setMessage(getString(R.string.are_sure_delete_entire_form)+" "+nameToDelete+"?")
				.setCancelable(true)
				.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						for (Entity entity: entities) {
							if (entity.getName().equals(nameToDelete)) {
								try {
									new EntityDao(sqlHelper.getWritableDatabase()).delete(entity);
                                    new AttributeDao(sqlHelper.getWritableDatabase()).deleteAttributesForEntity(entity);
                                    new DataDao(sqlHelper.getWritableDatabase()).deleteDataForEntity(entity);
								} finally {
									sqlHelper.close();
								}
								makeToast(getString(R.string.deleted_form)+" "+nameToDelete);
								break;
							}
						}
					}
				})
				.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
				AlertDialog alert = builder.create();
				alert.show();

			}
		});

		alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {

			}
		});

		alert.show();

	}

	private File getBlobExportDir() {

        File blobDir = new File(storagePath + "/blob_data");
        blobDir.mkdirs();
        return blobDir;
	}

	private String getBlobMetadataFileName() {
		return "_blob_metadata.csv";
	}

	private void doBlobImport() throws IOException {

		File blobdir = getBlobExportDir();
		File metadata = new File(blobdir, getBlobMetadataFileName());
		BufferedReader in = new BufferedReader(new FileReader(metadata));
		try {
            String header = in.readLine();
			SQLiteDatabase database = sqlHelper.getWritableDatabase();
			BlobDataDao blobDataDao = new BlobDataDao(database);
			try {

				int rowCount = 0;
				while (true) {
					rowCount++; // 1-based

					database.beginTransaction();
					try {

						BlobData blobdata = getMetadataRow(in, rowCount);
						if (blobdata == null) break;

						BlobData existingBlobdata = blobDataDao.getByGuid(blobdata.getGuid());
						if (existingBlobdata != null) blobdata.setId(existingBlobdata.getId());
						existingBlobdata = null; // avoid having two blobs' byte data in memory at once

						// Read import blob data
						File datfile = new File(blobdir, blobdata.getGuid()+".dat");
						if (datfile.exists()) {
							FileInputStream datin = new FileInputStream(datfile);
							byte[] b = new byte[datin.available()];
							datin.read(b);
							datin.close();
							blobdata.setBlobData(b);
						} 

						blobDataDao.save(blobdata);

						database.setTransactionSuccessful();
					} finally {
						database.endTransaction(); // try to make sure transaction ends for each blob so that a large rollback segment is not created depending on impl.
					}	
					
				}


			} finally {
				sqlHelper.close();
			}

		} finally {
			in.close();
		}

	}

	private BlobData getMetadataRow(BufferedReader in, int rowCount) throws IOException {

		Entity entity = new Entity();
		entity.setName(getBlobMetadataFileName());

		String[] line;
		while (true) {
			line = parseLine(in, rowCount, entity);
			if (line == null) return null; // eof
			if (line.length > 0) break; // not a blank line
		}

		if (line.length < 4) {
			throw new ParseException(getString(R.string.expected_metadata_fields_missing), rowCount, entity);
		}

		BlobData blobData = new BlobData();
		blobData.setGuid(line[0]);
		blobData.setFileName(line[1]);
		blobData.setMimeType(line[2]);
		blobData.setSize(Integer.parseInt(line[3]));

		return blobData;
	}

	private void doBlobExport() throws IOException {

        File blobDir = getBlobExportDir();

        // Delete any existing exported blob data first
        File[] files = blobDir.listFiles();
        for (File file: files) file.delete();

        File metadata = new File(blobDir, getBlobMetadataFileName());
        if (metadata.exists()) metadata.delete();
        Writer out = null;
        try{
            out = new BufferedWriter(new FileWriter(metadata));
        }catch (java.io.IOException ex){}


        // header
        String header = "\"giud\",\"fileName\",\"mimeType\",\"size\"\n";
        try{
            out.write(header);
        }catch (java.io.IOException ex){}


        SQLiteDatabase database = sqlHelper.getWritableDatabase();
        BlobDataDao blobDataDao = new BlobDataDao(database);
        try {

            Cursor cursor = database.query(BlobDataDao.TABLE_NAME, BlobDataDao.ALL_FIELDS, null, null, null, null, null);

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {

                BlobData data = blobDataDao.mapObject(cursor);

                String line = "\""+ data.getGuid()
                        +"\",\""+ csvEscape(data.getFileName())
                        +"\",\""+ csvEscape(data.getMimeType())
                        +"\",\""+ data.getSize()+"\"\n";

                try{
                    out.write(line);
                }catch (java.io.IOException ex){}


                File datfile = new File(blobDir, data.getGuid() + ".dat");
                if (datfile.exists()) datfile.delete();
                try{
                    FileOutputStream datout = new FileOutputStream(datfile);
                    datout.write(data.getBlobData());
                    datout.close();
                }catch (java.io.IOException ex){}


                cursor.moveToNext();
            }

            cursor.close();

        } finally {
            sqlHelper.close();
            try{
                out.flush();
                out.close();
            }catch (java.io.IOException ex){}
        }

	}

	private void doExport() {

        final Activity activity = this;
        checkStoragePermission(activity, new IGrantPermissionCallback() {
            @Override
            public void granted() {
                DialogUtils.displayChooseOkNoDialog(activity, R.string.allow_storage,
                        R.string.permission_granted, new IDialogClickListener() {
                            @Override
                            public void onOK() {
                                try {

                                    List<Entity> entities = getEntities();
                                    for (Entity entity: entities) {

                                        List<Attribute> attributes = getAttributes(entity);
                                        entity.setAttributes(attributes);
                                        File file = getEntityFile(entity);
                                        if (file.exists()) file.delete();
                                        Writer out = new BufferedWriter(new FileWriter(file));
                                        try {

                                            //header record
                                            out.write("\"_id\"");
                                            for (Attribute attribute: attributes) {
                                                out.write(",");
                                                out.write("\""+csvEscape(attribute.getAttributeName())+"\"");
                                            }
                                            out.write("\r\n");

                                            SQLiteDatabase database = sqlHelper.getWritableDatabase();
                                            DataDao dataDao = new DataDao(database);
                                            try {
                                                List<Map<String, String>> results = dataDao.searchNoLimits(entity, new HashMap<String, String>());
                                                for (Map<String, String> row: results) {
                                                    out.write("\""+row.get("_id")+"\"");
                                                    for (Attribute attribute: attributes) {
                                                        String name = attribute.getAttributeName();
                                                        String value = row.get(name);
                                                        if (value == null) value = "";
                                                        value = csvEscape(value);
                                                        String field = ",\"" + value + "\"";
                                                        out.write(field);
                                                    }
                                                    out.write("\r\n");
                                                }

                                            } finally {
                                                sqlHelper.close();
                                            }

                                        } finally {
                                            out.flush();
                                            out.close();
                                        }
                                    }

                                    doBlobExport();

                                    makeToast(getString(R.string.data_files_exported,entities.size()));

                                    doExportForms();

                                } catch (Exception e) {
                                    makeToast(e.getMessage());
                                }
                            }
                        });
            }

            @Override
            public void denied() {

            }
        });

	}

	private void doExportForms() {

		try {

			List<Entity> entities = getEntities();
			for (Entity entity: entities) {

				List<Attribute> attributes = getAttributes(entity);
				entity.setAttributes(attributes);
				File file = getFormExportFile(entity);
				if (file.exists()) file.delete();
				Writer out = new BufferedWriter(new FileWriter(file));
				try {

					//header record
					out.write("\"entityName\"");
					out.write(",\"attributeName\"");
					out.write(",\"attributeDesc\"");
					out.write(",\"dataType\"");
					out.write(",\"refEntityName\"");
					out.write(",\"isPrimaryKeyPart\"");
					out.write(",\"isRequired\"");
					out.write(",\"isSearchable\"");
					out.write(",\"isListable\"");
					out.write(",\"isEntityDescription\"");
					out.write(",\"displayOrder\"");
					out.write(",\"choices\"");
					out.write(",\"validationRegex\"");
					out.write(",\"validationExample\"");
					out.write("\r\n");

					for (Attribute attribute: attributes) {
						out.write("\""+csvEscape(attribute.getEntityName())+"\"");
						out.write(",\""+csvEscape(attribute.getAttributeName())+"\"");
						out.write(",\""+csvEscape(attribute.getAttributeDesc())+"\"");
						out.write(",\""+csvEscape(attribute.getDataType())+"\"");
						out.write(",\""+csvEscape(attribute.getRefEntityName())+"\"");
						out.write(",\""+attribute.isPrimaryKeyPart()+"\"");
						out.write(",\""+attribute.isRequired()+"\"");
						out.write(",\""+attribute.isSearchable()+"\"");
						out.write(",\""+attribute.isListable()+"\"");
						out.write(",\""+attribute.isEntityDescription()+"\"");
						out.write(",\""+attribute.getDisplayOrder()+"\"");
						out.write(",\""+csvEscape(attribute.getChoices())+"\"");
						out.write(",\""+csvEscape(attribute.getValidationRegex())+"\"");
						out.write(",\""+csvEscape(attribute.getValidationExample())+"\"");
						out.write("\r\n");
					}

				} finally {
					out.flush();
					out.close();
				}
			}

			makeToast(getString(R.string.form_def_files_exported,entities.size()));

		} catch (Exception e) {
			makeToast(e.getMessage());
		}
	}


	private String csvEscape(String s) {
		if (s == null) return "";
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < s.length();i++) {
			char c = s.charAt(i);
			if (c == '\"') {
				sb.append("\"\"");
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	private File getEntityFile(Entity entity) {
		new File(storagePath).mkdirs();
		return new File(storagePath + "/urf_" + entity.getName()+".csv");
	}

	private static final String FORM_DEF_PREFIX = "urformdef_";
	private File getFormExportFile(Entity entity) {
		new File(storagePath).mkdirs();
		return new File(storagePath + "/"+FORM_DEF_PREFIX + entity.getName()+".csv");
	}

	private void doImport() {

		try {

			int fileCount = 0;

			List<Entity> entities = getEntities();
			for (Entity entity: entities) {

				List<Attribute> attributes = getAttributes(entity);
				File file = getEntityFile(entity);
				if (!file.exists()) continue;
				BufferedReader in = new BufferedReader(new FileReader(file));
				fileCount++;
				try {

					SQLiteDatabase database = sqlHelper.getWritableDatabase();
					DataDao dataDao = new DataDao(database);
					try {
						Map<String, String> headerMap = null;
						int rowCount = 0;
						while (true) {
							rowCount++; // 1-based
							Map<String, String> row = getRow(attributes, in, rowCount, entity);
							if (row == null) break;
							if (rowCount == 1) {
								// validate input columns
								headerMap = row;
								for (Attribute attribute: attributes) {
									String attributeName = attribute.getAttributeName();
									String colHeader = headerMap.get(attributeName);
									if (!attributeName.equals(colHeader)) {
										if (colHeader == null) {
											throw new ParseException(getString(R.string.missing_column) +" "+attributeName, rowCount, entity);
										} else {
											throw new ParseException(getString(R.string.invalid_column) +" " +colHeader+", "+ getString(R.string.expected) +" "+attributeName, rowCount, entity);
										}
									}
								}
							} else {
								dataDao.saveEntityData(entity.getName(), row);
							}
						}
					} finally {
						sqlHelper.close();
					}

				} finally {
					in.close();
				}

			}

			doBlobImport();

			makeToast(getString(R.string.data_files_imported,fileCount));


		} catch (Exception e) {
			makeToast(e.getMessage());
		}

	}

	private void doImportForms() {

		try {

			List<Entity> existingEntities = getEntities();
			List<Entity> newEntities = new ArrayList<Entity>();

			List<File> processFiles = new ArrayList<File>();
			File dir = new File(storagePath);
			File[] list = dir.listFiles();
			for (File f: list) {
				if (f.getName().startsWith(FORM_DEF_PREFIX)) {
					processFiles.add(f);
				}
			}

			// Parse files
			int fileCount = 0;
			for (File file: processFiles) {

				String entityName = file.getName().substring(FORM_DEF_PREFIX.length());
				entityName = entityName.substring(0, entityName.indexOf("."));
				Entity entity = new Entity();
				entity.setName(entityName);

				List<Attribute> newAttributes = new ArrayList<Attribute>();
				BufferedReader in = new BufferedReader(new FileReader(file));
				try {
                    String header = in.readLine();
					int rowCount = 0;
					Attribute attribute;
					while ((attribute = getFormImportRow(in, rowCount, entity)) != null) {
						newAttributes.add(attribute);
					}
				} finally {
					in.close();
				}

				entity.setAttributes(newAttributes);
				newEntities.add(entity);

				fileCount++;

			}

			// Save entities and attributes
			SQLiteDatabase db = sqlHelper.getWritableDatabase();
			try {
				for (Entity entity: newEntities) {
					// Delete any existing entity attributes
					boolean found = false;
					for (Entity existingentity: existingEntities) {
						if (existingentity.getName().equals(entity.getName())) {
							found = true;
							AttributeDao attributeDao = new AttributeDao(db);
							List<Attribute> deleteattributes = attributeDao.list(existingentity);
							for (Attribute attribute: deleteattributes) attributeDao.delete(attribute);
						}
					}
					if (!found) {
						EntityDao entityDao = new EntityDao(db);
						entityDao.save(entity);
					}
					AttributeDao attributeDao = new AttributeDao(db);
					for (Attribute attribute: entity.getAttributes()) {
						attributeDao.save(attribute);
					}
				}
			} finally {
				sqlHelper.close();
			}

			makeToast(getString(R.string.form_def_files_imported,fileCount));


		} catch (Exception e) {
			makeToast(e.getMessage());
		}

	}

	private String[] parseLine(BufferedReader in, int rowCount, Entity entity) throws IOException {

		String line = in.readLine();
		if (line == null) return null;
		line = line.trim();
		String concatLine = line;
		while (!line.endsWith("\"")) {  // continuation line
			line = in.readLine();
			if (line == null) break;
			line = line.trim();
			concatLine = concatLine + "\n" + line;
		}
		if (!concatLine.endsWith("\"")) throw new ParseException(getString(R.string.invalid_file_format), rowCount, entity);

		List<String> fields = new ArrayList<String>();
		boolean inQuote = false;
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < concatLine.length(); i++) {
			char c = concatLine.charAt(i);
			if (inQuote) {
				if (c == '\"') {
					if (sb.length() > 0 && i < concatLine.length()-1 && concatLine.charAt(i+1) == '\"') {
						sb.append(c);
						i++;
					} else {
						fields.add(sb.toString());
						sb = new StringBuffer();
						inQuote = false;
					}
				} else {
					sb.append(c);
				}
			} else {
				if (c == '\"') {
					inQuote = true;
				} else if (c == ',' || c == ' ') {
				} else {
					throw new ParseException(getString(R.string.all_fields_quotes), rowCount, entity);
				}
			}
		}

		return fields.toArray(new String[fields.size()]);
	}

	private Map<String, String> getRow(List<Attribute> attributes, BufferedReader in, int rowCount, Entity entity) throws IOException {

		String[] line;
		while (true) {
			line = parseLine(in, rowCount, entity);
			if (line == null) return null; // eof
			if (line.length > 0) break; // not a blank line
		}

		if (line.length != attributes.size()+1) {
			throw new ParseException(getString(R.string.expected_num_fields,attributes.size()+1), rowCount, entity);
		}

		Map<String, String> row = new HashMap<String, String>();
		row.put("_id", line[0]);
		for (int i = 0; i < attributes.size(); i++) {
			Attribute attribute = attributes.get(i);
			row.put(attribute.getAttributeName(), line[i+1]);
		}

		return row;
	}

	private Attribute getFormImportRow(BufferedReader in, int rowCount, Entity entity) throws IOException {

		String[] line;
		while (true) {
			line = parseLine(in, rowCount, entity);
			if (line == null) return null; // eof
			if (line.length > 0) break; // not a blank line
		}

		Attribute attribute = new Attribute();
		try {
			attribute.setEntityName(line[0]);
			attribute.setAttributeName(line[1]);
			attribute.setAttributeDesc(line[2]);
			attribute.setDataType(line[3]);
			attribute.setRefEntityName(line[4]);
			attribute.setPrimaryKeyPart("true".equalsIgnoreCase(line[5]));
			attribute.setRequired("true".equalsIgnoreCase(line[6]));
			attribute.setSearchable("true".equalsIgnoreCase(line[7]));
			attribute.setListable("true".equalsIgnoreCase(line[8]));
			attribute.setEntityDescription("true".equalsIgnoreCase(line[9]));
			String sorder = line[10];
			if (sorder.trim().length() == 0) sorder = "0";
			attribute.setDisplayOrder(Integer.valueOf(sorder));
			attribute.setChoices(line[11]);
			attribute.setValidationRegex(line[12]);
			attribute.setValidationExample(line[13]);
		} catch (Exception e) {
			throw new ParseException(getString(R.string.invalid_form_def,e.getMessage()), rowCount, entity);
		}

		if (!entity.getName().equals(attribute.getEntityName())) {
			throw new ParseException(getString(R.string.invalid_entity_name), rowCount, entity);
		}

		return attribute;

	}

	public final class ParseException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public ParseException(String message, int rowCount, Entity entity) {
			super(getString(R.string.parse_exception_message,message,rowCount,entity.getName()));
		}
	}

}
