/*
 * Copyright 2018 Murat Artim (muratartim@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package equinox.task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.fileType.STFFileBucket;
import equinox.data.fileType.SpectrumItem;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for get contribution names task.
 *
 * @author Murat Artim
 * @date Apr 15, 2015
 * @time 9:15:11 PM
 */
public class GetContributionNames extends InternalEquinoxTask<ArrayList<String>> implements ShortRunningTask {

	/** Requesting object. */
	private final DamageContributionRequester panel_;

	/** Spectrum items. This can be either damage contributions or STF file buckets. */
	private final ArrayList<SpectrumItem> items_;

	/**
	 * Creates get contribution names task.
	 *
	 * @param panel
	 *            Requesting object. This can be null.
	 * @param items
	 *            Spectrum items. This can be either damage contributions or STF file buckets.
	 */
	public GetContributionNames(DamageContributionRequester panel, ArrayList<SpectrumItem> items) {
		panel_ = panel;
		items_ = items;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Get damage contribution names";
	}

	@Override
	protected ArrayList<String> call() throws Exception {

		// update progress info
		updateTitle("Retrieving damage contribution names...");

		// STF file buckets
		if (items_.get(0) instanceof STFFileBucket)
			return getFromBuckets();

		// contributions
		return getFromContributions();
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set file info
		try {
			if (panel_ != null) {
				panel_.setContributions(get());
			}
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Gets contribution names from input damage contributions.
	 *
	 * @return Contribution names.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private ArrayList<String> getFromContributions() throws Exception {

		// create lists
		ArrayList<String> names = new ArrayList<>();

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// prepare statement
			String sql = "select distinct name from dam_contribution where ";
			String[] ids = new String[items_.size()];
			for (int i = 0; i < ids.length; i++) {
				ids[i] = "contributions_id = " + items_.get(i).getID();
			}
			sql += String.join(" or ", ids) + " order by name";

			// create statement
			try (Statement statement = connection.createStatement()) {

				// get contribution names of first item
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						names.add(resultSet.getString("name"));
					}
				}
			}
		}

		// return names
		return names;
	}

	/**
	 * Gets contribution names from input STF file buckets.
	 *
	 * @return Contribution names.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private ArrayList<String> getFromBuckets() throws Exception {

		// create lists
		boolean firstNamesSet = false;
		ArrayList<String> names = new ArrayList<>(), names1 = new ArrayList<>(), toBeRemoved = new ArrayList<>();

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// prepare statement for getting STF files
			String sql = "select file_id from stf_files where cdf_id = ?";
			try (PreparedStatement getSTFIDs = connection.prepareStatement(sql)) {

				// prepare statement for getting contribution IDs
				sql = "select contributions_id from dam_contributions where stf_id = ?";
				try (PreparedStatement getContributionIDs = connection.prepareStatement(sql)) {

					// prepare statement to get contribution names
					sql = "select name from dam_contribution where contributions_id = ? order by name";
					try (PreparedStatement getContributionNames = connection.prepareStatement(sql)) {

						// loop over buckets
						for (SpectrumItem item : items_) {

							// cast to bucket
							STFFileBucket bucket = (STFFileBucket) item;

							// get STF IDs
							getSTFIDs.setInt(1, bucket.getParentItem().getID());
							try (ResultSet stfIDs = getSTFIDs.executeQuery()) {

								// loop over STF IDs
								while (stfIDs.next()) {

									// get contribution IDs
									getContributionIDs.setInt(1, stfIDs.getInt("file_id"));
									try (ResultSet contributionIDs = getContributionIDs.executeQuery()) {

										// loop over contribution IDs
										while (contributionIDs.next()) {

											// get contribution names
											getContributionNames.setInt(1, contributionIDs.getInt("contributions_id"));
											try (ResultSet contributionNames = getContributionNames.executeQuery()) {

												// get names of first contribution
												if (!firstNamesSet) {
													while (contributionNames.next()) {
														names.add(contributionNames.getString("name"));
													}
													firstNamesSet = true;
												}

												// get names of remaining contributions
												else {

													// get names
													names1.clear();
													while (contributionNames.next()) {
														names1.add(contributionNames.getString("name"));
													}

													// get to-be-removed contributions
													toBeRemoved.clear();
													for (String name : names) {
														if (!names1.contains(name)) {
															toBeRemoved.add(name);
														}
													}

													// remove contributions
													names.removeAll(toBeRemoved);
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}

		// return names
		return names;
	}

	/**
	 * Interface for damage contribution requesting panels.
	 *
	 * @author Murat Artim
	 * @date Sep 3, 2015
	 * @time 2:02:03 PM
	 */
	public interface DamageContributionRequester {

		/**
		 * Sets contributions to this panel.
		 *
		 * @param contributions
		 *            Contributions to be set.
		 */
		void setContributions(ArrayList<String> contributions);
	}
}
