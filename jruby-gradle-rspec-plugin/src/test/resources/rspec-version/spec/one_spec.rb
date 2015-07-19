describe 'RSpecVersion' do
  it 'uses the right rspec version' do
    require 'rspec'
    expect(Gem.loaded_specs['rspec'].version.to_s).to eq '3.2.0'
  end
end
